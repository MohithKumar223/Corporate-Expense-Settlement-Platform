package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.*;
import com.company.expenseplatform.entity.enums.*;
import com.company.expenseplatform.repository.ExpenseRepository;
import com.company.expenseplatform.repository.ExceptionRecordRepository;
import com.company.expenseplatform.repository.ExpensePolicyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ExpensePolicyRepository policyRepository;
    private final ExceptionRecordRepository exceptionRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "pdf", "doc", "docx", "xls", "xlsx");

    public ExpenseService(ExpenseRepository expenseRepository,
                          ExpensePolicyRepository policyRepository,
                          ExceptionRecordRepository exceptionRepository,
                          AuditService auditService,
                          NotificationService notificationService) {
        this.expenseRepository = expenseRepository;
        this.policyRepository = policyRepository;
        this.exceptionRepository = exceptionRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public Optional<Expense> findById(Long id) {
        return expenseRepository.findById(id);
    }

    public List<Expense> findByEmployee(Long employeeId) {
        return expenseRepository.findByEmployeeId(employeeId);
    }

    public List<Expense> findByAdvanceRequest(Long arId) {
        return expenseRepository.findByAdvanceRequestId(arId);
    }

    public List<Expense> findAll() {
        return expenseRepository.findAll();
    }

    /**
     * Submit a new expense with full validation.
     * Returns a list of validation warnings/errors; empty list means clean submission.
     */
    @Transactional
    public List<String> submitExpense(Expense expense, MultipartFile receipt) {
        List<String> warnings = new ArrayList<>();

        // ── Basic validation ────────────────────────────────────────────────────
        if (expense.getAmount() == null || expense.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Expense amount must be positive");
        }
        if (expense.getExpenseDate() == null) {
            throw new RuntimeException("Expense date is required");
        }
        if (expense.getExpenseDate().isAfter(LocalDate.now())) {
            throw new RuntimeException("Expense date cannot be in the future");
        }

        // ── Duplicate detection ─────────────────────────────────────────────────
        List<Expense> duplicates = expenseRepository.findPotentialDuplicates(
                expense.getEmployee().getId(), expense.getCategory(),
                expense.getExpenseDate(), expense.getAmount());
        if (!duplicates.isEmpty()) {
            ExceptionRecord ex = new ExceptionRecord();
            ex.setExpense(expense);
            ex.setAdvanceRequest(expense.getAdvanceRequest());
            ex.setType(ExceptionType.DUPLICATE_EXPENSE);
            ex.setDescription("Potential duplicate: same employee, category, date, and amount as expense #" + duplicates.get(0).getId());
            exceptionRepository.save(ex);
            warnings.add("Potential duplicate expense detected");
        }

        // ── Policy validation ───────────────────────────────────────────────────
        Optional<ExpensePolicy> policyOpt = policyRepository.findByCategoryNameAndActiveTrue(expense.getCategory());
        if (policyOpt.isPresent()) {
            ExpensePolicy policy = policyOpt.get();

            // Max amount check
            if (expense.getAmount().compareTo(policy.getMaxAmount()) > 0) {
                ExceptionRecord ex = new ExceptionRecord();
                ex.setExpense(expense);
                ex.setAdvanceRequest(expense.getAdvanceRequest());
                ex.setType(ExceptionType.POLICY_VIOLATION);
                ex.setDescription("Amount ₹" + expense.getAmount() + " exceeds policy max ₹" + policy.getMaxAmount() + " for " + expense.getCategory());
                exceptionRepository.save(ex);
                warnings.add("Amount exceeds policy limit of ₹" + policy.getMaxAmount());
            }

            // Daily limit check
            if (policy.getDailyLimit() != null) {
                BigDecimal dailyTotal = expenseRepository.sumDailyExpenseByCategory(
                        expense.getEmployee().getId(), expense.getExpenseDate(), expense.getCategory());
                BigDecimal newTotal = dailyTotal.add(expense.getAmount());
                if (newTotal.compareTo(policy.getDailyLimit()) > 0) {
                    ExceptionRecord ex = new ExceptionRecord();
                    ex.setExpense(expense);
                    ex.setAdvanceRequest(expense.getAdvanceRequest());
                    ex.setType(ExceptionType.POLICY_VIOLATION);
                    ex.setDescription("Daily total ₹" + newTotal + " exceeds daily limit ₹" + policy.getDailyLimit());
                    exceptionRepository.save(ex);
                    warnings.add("Daily spending limit exceeded");
                }
            }

            // Receipt requirement
            if (policy.isRequiresReceipt() && (receipt == null || receipt.isEmpty())) {
                ExceptionRecord ex = new ExceptionRecord();
                ex.setExpense(expense);
                ex.setAdvanceRequest(expense.getAdvanceRequest());
                ex.setType(ExceptionType.MISSING_RECEIPT);
                ex.setDescription("Receipt required for category: " + expense.getCategory());
                exceptionRepository.save(ex);
                warnings.add("Receipt is required for this category");
            }
        }

        // ── Advance association check ───────────────────────────────────────────
        if (expense.getAdvanceRequest() != null) {
            AdvanceRequest ar = expense.getAdvanceRequest();
            if (!ar.getEmployee().getId().equals(expense.getEmployee().getId())) {
                ExceptionRecord ex = new ExceptionRecord();
                ex.setExpense(expense);
                ex.setAdvanceRequest(ar);
                ex.setType(ExceptionType.INVALID_ADVANCE_ASSOCIATION);
                ex.setDescription("Expense employee does not match advance request employee");
                exceptionRepository.save(ex);
                warnings.add("Advance request does not belong to this employee");
            }
        }

        // ── Receipt upload ──────────────────────────────────────────────────────
        if (receipt != null && !receipt.isEmpty()) {
            String filePath = saveReceipt(receipt, expense.getEmployee().getId());
            expense.setReceiptPath(filePath);
        }

        expense.setStatus(ExpenseStatus.SUBMITTED);
        Expense saved = expenseRepository.save(expense);

        auditService.log("EXPENSE_SUBMITTED", "Expense", saved.getId(),
                "Expense submitted: ₹" + saved.getAmount() + " for " + saved.getCategory());
        return warnings;
    }

    @Transactional
    public Expense validate(Long expenseId, String notes) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));

        expense.setStatus(ExpenseStatus.VALIDATED);
        expense.setValidationNotes(notes);
        Expense saved = expenseRepository.save(expense);

        notificationService.send(expense.getEmployee(),
                "Your expense #" + expense.getId() + " for ₹" + expense.getAmount() + " has been validated",
                NotificationType.EXPENSE_VALIDATED, expense.getId(), "Expense");

        auditService.log("EXPENSE_VALIDATED", "Expense", saved.getId(),
                "Expense validated. Notes: " + notes);
        return saved;
    }

    @Transactional
    public Expense approve(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));

        expense.setStatus(ExpenseStatus.APPROVED);
        Expense saved = expenseRepository.save(expense);

        auditService.log("EXPENSE_APPROVED", "Expense", saved.getId(), "Expense approved");
        return saved;
    }

    @Transactional
    public Expense reject(Long expenseId, String reason) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));

        expense.setStatus(ExpenseStatus.REJECTED);
        expense.setValidationNotes(reason);
        Expense saved = expenseRepository.save(expense);

        notificationService.send(expense.getEmployee(),
                "Your expense #" + expense.getId() + " has been rejected. Reason: " + reason,
                NotificationType.EXPENSE_REJECTED, expense.getId(), "Expense");

        auditService.log("EXPENSE_REJECTED", "Expense", saved.getId(), "Rejected: " + reason);
        return saved;
    }

    @Transactional
    public Expense dispute(Long expenseId, String reason) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + expenseId));

        expense.setStatus(ExpenseStatus.DISPUTED);
        expense.setValidationNotes(reason);
        Expense saved = expenseRepository.save(expense);

        ExceptionRecord ex = new ExceptionRecord();
        ex.setExpense(expense);
        ex.setAdvanceRequest(expense.getAdvanceRequest());
        ex.setType(ExceptionType.DISPUTED_EXPENSE);
        ex.setDescription("Disputed: " + reason);
        exceptionRepository.save(ex);

        auditService.log("EXPENSE_DISPUTED", "Expense", saved.getId(), "Disputed: " + reason);
        return saved;
    }

    public BigDecimal sumValidatedByAdvanceRequest(Long arId) {
        return expenseRepository.sumAmountByAdvanceRequestAndStatuses(arId,
                List.of(ExpenseStatus.VALIDATED, ExpenseStatus.APPROVED));
    }

    public long countByEmployee(Long employeeId) {
        return expenseRepository.countByEmployeeId(employeeId);
    }

    public long countByStatus(ExpenseStatus status) {
        return expenseRepository.countByStatus(status);
    }

    public Map<String, BigDecimal> sumByCategory() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : expenseRepository.sumExpensesByCategory()) {
            if (row != null && row.length >= 2 && row[0] != null) {
                map.put(row[0].toString(), (BigDecimal) row[1]);
            }
        }
        return map;
    }

    public Map<String, BigDecimal> sumByDepartment() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : expenseRepository.sumExpensesByDepartment()) {
            if (row != null && row.length >= 2 && row[0] != null) {
                map.put(row[0].toString(), (BigDecimal) row[1]);
            }
        }
        return map;
    }

    public Map<String, BigDecimal> sumByMonth() {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        String[] monthNames = {"", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        for (Object[] row : expenseRepository.sumExpensesByMonth()) {
            if (row != null && row.length >= 3 && row[0] != null) {
                int month = ((Number) row[0]).intValue();
                String label = (month >= 1 && month <= 12) ? monthNames[month] : "M" + month;
                map.put(label, (BigDecimal) row[2]);
            }
        }
        return map;
    }

    public BigDecimal sumTotalValidated() {
        return expenseRepository.sumTotalByStatuses(List.of(ExpenseStatus.VALIDATED, ExpenseStatus.APPROVED));
    }

    private String saveReceipt(MultipartFile file, Long employeeId) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "file";
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new RuntimeException("File type not allowed: " + ext);
        }

        String fileName = "emp" + employeeId + "_" + System.currentTimeMillis() + "." + ext;
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save receipt: " + e.getMessage());
        }
    }
}
