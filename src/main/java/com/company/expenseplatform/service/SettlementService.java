package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.*;
import com.company.expenseplatform.entity.enums.*;
import com.company.expenseplatform.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core Settlement Engine.
 *
 * Settlement Difference = Total Validated Expenses − Total Disbursed Advance.
 *
 * Case A: Unused advance  → employee must return (recovery)
 * Case B: Exact match     → zero balance
 * Case C: Over-spent      → company owes additional reimbursement
 *
 * All monetary calculations use BigDecimal. Never uses double/float.
 */
@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final AdvanceDisbursementRepository disbursementRepository;
    private final ExpenseRepository expenseRepository;
    private final RecoveryTransactionRepository recoveryRepository;
    private final AdvanceRequestRepository advanceRequestRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    /** Only these expense statuses count toward settlement */
    private static final List<ExpenseStatus> VALID_EXPENSE_STATUSES =
            List.of(ExpenseStatus.VALIDATED, ExpenseStatus.APPROVED);

    public SettlementService(SettlementRepository settlementRepository,
                             AdvanceDisbursementRepository disbursementRepository,
                             ExpenseRepository expenseRepository,
                             RecoveryTransactionRepository recoveryRepository,
                             AdvanceRequestRepository advanceRequestRepository,
                             AuditService auditService,
                             NotificationService notificationService) {
        this.settlementRepository = settlementRepository;
        this.disbursementRepository = disbursementRepository;
        this.expenseRepository = expenseRepository;
        this.recoveryRepository = recoveryRepository;
        this.advanceRequestRepository = advanceRequestRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public Optional<Settlement> findById(Long id) {
        return settlementRepository.findById(id);
    }

    public Optional<Settlement> findByAdvanceRequest(Long arId) {
        return settlementRepository.findByAdvanceRequestId(arId);
    }

    public List<Settlement> findAll() {
        return settlementRepository.findAll();
    }

    public List<Settlement> findByStatus(SettlementStatus status) {
        return settlementRepository.findByStatus(status);
    }

    public List<Settlement> findByEmployee(Long employeeId) {
        return settlementRepository.findByEmployeeId(employeeId);
    }

    /**
     * Calculate or recalculate the settlement for an advance request.
     * Idempotent — creates if not exists, updates if already calculated.
     * Prevents double settlement by checking completion status.
     */
    @Transactional
    public Settlement calculateSettlement(Long advanceRequestId) {
        AdvanceRequest ar = advanceRequestRepository.findById(advanceRequestId)
                .orElseThrow(() -> new RuntimeException("Advance request not found: " + advanceRequestId));

        // Must be disbursed before settlement
        if (ar.getStatus() != AdvanceRequestStatus.DISBURSED &&
                ar.getStatus() != AdvanceRequestStatus.PARTIALLY_SETTLED) {
            throw new RuntimeException("Advance must be DISBURSED before settlement calculation");
        }

        // Prevent recalculation of completed settlements
        Optional<Settlement> existingOpt = settlementRepository.findByAdvanceRequestId(advanceRequestId);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == SettlementStatus.COMPLETED) {
            throw new RuntimeException("Settlement is already COMPLETED and cannot be recalculated");
        }

        // ── Core calculation (BigDecimal only) ──────────────────────────────────

        BigDecimal totalDisbursed = disbursementRepository.sumDisbursedByAdvanceRequest(advanceRequestId);

        BigDecimal totalValidatedExpenses = expenseRepository.sumAmountByAdvanceRequestAndStatuses(
                advanceRequestId, VALID_EXPENSE_STATUSES);

        // Settlement Difference = Total Validated Expenses − Total Disbursed
        BigDecimal settlementDifference = totalValidatedExpenses.subtract(totalDisbursed);

        BigDecimal recoveryAmount = BigDecimal.ZERO;
        BigDecimal reimbursementAmount = BigDecimal.ZERO;

        int comparison = settlementDifference.compareTo(BigDecimal.ZERO);

        if (comparison < 0) {
            // Case A: Unused advance — employee owes money back
            recoveryAmount = settlementDifference.negate(); // Make positive
        } else if (comparison > 0) {
            // Case C: Over-spent — company owes employee
            reimbursementAmount = settlementDifference;
        }
        // Case B: comparison == 0 → exact match, both remain zero

        // ── Persist settlement ──────────────────────────────────────────────────

        Settlement settlement = existingOpt.orElseGet(() -> {
            Settlement s = new Settlement();
            s.setAdvanceRequest(ar);
            return s;
        });

        settlement.setTotalDisbursed(totalDisbursed);
        settlement.setTotalValidatedExpenses(totalValidatedExpenses);
        settlement.setSettlementDifference(settlementDifference);
        settlement.setRecoveryAmount(recoveryAmount);
        settlement.setReimbursementAmount(reimbursementAmount);
        settlement.setCalculatedAt(LocalDateTime.now());

        // Determine status based on amounts
        if (recoveryAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Check if already fully recovered
            BigDecimal totalRecovered = settlement.getTotalRecovered();
            if (totalRecovered.compareTo(recoveryAmount) >= 0) {
                settlement.setStatus(SettlementStatus.COMPLETED);
                settlement.setCompletedAt(LocalDateTime.now());
            } else {
                settlement.setStatus(SettlementStatus.PENDING_RECOVERY);
            }
        } else if (reimbursementAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalReimbursed = settlement.getTotalReimbursed();
            if (totalReimbursed.compareTo(reimbursementAmount) >= 0) {
                settlement.setStatus(SettlementStatus.COMPLETED);
                settlement.setCompletedAt(LocalDateTime.now());
            } else {
                settlement.setStatus(SettlementStatus.PENDING_REIMBURSEMENT);
            }
        } else {
            // Exact match
            settlement.setStatus(SettlementStatus.COMPLETED);
            settlement.setCompletedAt(LocalDateTime.now());
        }

        Settlement saved = settlementRepository.save(settlement);

        // Update advance request status
        if (saved.getStatus() == SettlementStatus.COMPLETED) {
            ar.setStatus(AdvanceRequestStatus.SETTLED);
        } else {
            ar.setStatus(AdvanceRequestStatus.PARTIALLY_SETTLED);
        }
        advanceRequestRepository.save(ar);

        // Notify employee
        notificationService.send(ar.getEmployee(),
                "Settlement calculated for advance #" + ar.getId() +
                        ". Disbursed: ₹" + totalDisbursed +
                        ", Expenses: ₹" + totalValidatedExpenses +
                        ", Difference: ₹" + settlementDifference,
                NotificationType.SETTLEMENT_CALCULATED, saved.getId(), "Settlement");

        auditService.log("SETTLEMENT_CALCULATED", "Settlement", saved.getId(),
                "Disbursed=₹" + totalDisbursed + ", Expenses=₹" + totalValidatedExpenses +
                        ", Recovery=₹" + recoveryAmount + ", Reimbursement=₹" + reimbursementAmount);

        return saved;
    }

    /**
     * Record a recovery (employee repayment) against a settlement.
     */
    @Transactional
    public RecoveryTransaction recordRecovery(Long settlementId, RecoveryTransaction recovery) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));

        if (settlement.getStatus() == SettlementStatus.COMPLETED) {
            throw new RuntimeException("Settlement is already completed");
        }

        if (settlement.getRecoveryAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No recovery required for this settlement");
        }

        // Validate recovery amount
        BigDecimal outstanding = settlement.getRecoveryAmount().subtract(settlement.getTotalRecovered());
        if (recovery.getAmountRecovered().compareTo(outstanding) > 0) {
            throw new RuntimeException("Recovery amount ₹" + recovery.getAmountRecovered() +
                    " exceeds outstanding balance ₹" + outstanding);
        }

        recovery.setSettlement(settlement);
        RecoveryTransaction saved = recoveryRepository.save(recovery);

        // Update settlement totals
        BigDecimal newTotalRecovered = settlement.getTotalRecovered().add(recovery.getAmountRecovered());
        settlement.setTotalRecovered(newTotalRecovered);

        if (newTotalRecovered.compareTo(settlement.getRecoveryAmount()) >= 0) {
            settlement.setStatus(SettlementStatus.COMPLETED);
            settlement.setCompletedAt(LocalDateTime.now());

            // Update advance request
            AdvanceRequest ar = settlement.getAdvanceRequest();
            ar.setStatus(AdvanceRequestStatus.SETTLED);
            advanceRequestRepository.save(ar);
        }
        settlementRepository.save(settlement);

        auditService.log("RECOVERY_RECORDED", "RecoveryTransaction", saved.getId(),
                "Recovered ₹" + saved.getAmountRecovered() + " for settlement #" + settlementId);

        return saved;
    }

    /**
     * Record a reimbursement paid to the employee.
     */
    @Transactional
    public void recordReimbursement(Long settlementId, BigDecimal amount) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));

        if (settlement.getReimbursementAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("No reimbursement required for this settlement");
        }

        BigDecimal outstanding = settlement.getReimbursementAmount().subtract(settlement.getTotalReimbursed());
        if (amount.compareTo(outstanding) > 0) {
            throw new RuntimeException("Reimbursement amount exceeds outstanding balance");
        }

        settlement.setTotalReimbursed(settlement.getTotalReimbursed().add(amount));

        if (settlement.getTotalReimbursed().compareTo(settlement.getReimbursementAmount()) >= 0) {
            settlement.setStatus(SettlementStatus.COMPLETED);
            settlement.setCompletedAt(LocalDateTime.now());

            AdvanceRequest ar = settlement.getAdvanceRequest();
            ar.setStatus(AdvanceRequestStatus.SETTLED);
            advanceRequestRepository.save(ar);
        }
        settlementRepository.save(settlement);

        auditService.log("REIMBURSEMENT_RECORDED", "Settlement", settlementId,
                "Reimbursed ₹" + amount);
    }

    // ── Analytics ───────────────────────────────────────────────────────────────

    public long countByStatus(SettlementStatus status) {
        return settlementRepository.countByStatus(status);
    }

    public BigDecimal sumPendingRecovery() {
        return settlementRepository.sumPendingRecoveryAmounts();
    }

    public BigDecimal sumPendingReimbursement() {
        return settlementRepository.sumPendingReimbursementAmounts();
    }

    public List<RecoveryTransaction> getRecoveryTransactions(Long settlementId) {
        return recoveryRepository.findBySettlementId(settlementId);
    }

    public BigDecimal sumTotalRecovered() {
        return recoveryRepository.sumTotalRecovered();
    }
}
