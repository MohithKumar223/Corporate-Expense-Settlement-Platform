package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.ExceptionRecord;
import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.enums.ExceptionStatus;
import com.company.expenseplatform.repository.ExceptionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExceptionService {

    private final ExceptionRecordRepository exceptionRepository;
    private final AuditService auditService;

    public ExceptionService(ExceptionRecordRepository exceptionRepository, AuditService auditService) {
        this.exceptionRepository = exceptionRepository;
        this.auditService = auditService;
    }

    public List<ExceptionRecord> findAll() {
        return exceptionRepository.findAll();
    }

    public Optional<ExceptionRecord> findById(Long id) {
        return exceptionRepository.findById(id);
    }

    public List<ExceptionRecord> findByStatus(ExceptionStatus status) {
        return exceptionRepository.findByStatus(status);
    }

    public List<ExceptionRecord> findByExpense(Long expenseId) {
        return exceptionRepository.findByExpenseId(expenseId);
    }

    public long countOpen() {
        return exceptionRepository.countByStatus(ExceptionStatus.OPEN);
    }

    @Transactional
    public ExceptionRecord investigate(Long id, Employee investigator) {
        ExceptionRecord ex = exceptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exception not found: " + id));
        ex.setStatus(ExceptionStatus.UNDER_INVESTIGATION);
        ex.setInvestigatedBy(investigator);
        ExceptionRecord saved = exceptionRepository.save(ex);
        auditService.log("EXCEPTION_INVESTIGATED", "ExceptionRecord", id,
                "Investigation started by " + investigator.getFullName());
        return saved;
    }

    @Transactional
    public ExceptionRecord resolve(Long id, String resolution) {
        ExceptionRecord ex = exceptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exception not found: " + id));
        ex.setStatus(ExceptionStatus.RESOLVED);
        ex.setResolution(resolution);
        ex.setResolvedAt(LocalDateTime.now());
        ExceptionRecord saved = exceptionRepository.save(ex);
        auditService.log("EXCEPTION_RESOLVED", "ExceptionRecord", id, "Resolution: " + resolution);
        return saved;
    }

    @Transactional
    public ExceptionRecord close(Long id) {
        ExceptionRecord ex = exceptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exception not found: " + id));
        ex.setStatus(ExceptionStatus.CLOSED);
        ExceptionRecord saved = exceptionRepository.save(ex);
        auditService.log("EXCEPTION_CLOSED", "ExceptionRecord", id, "Exception closed");
        return saved;
    }
}
