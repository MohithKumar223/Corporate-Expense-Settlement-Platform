package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.*;
import com.company.expenseplatform.entity.enums.*;
import com.company.expenseplatform.repository.AdvanceRequestRepository;
import com.company.expenseplatform.repository.ApprovalHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AdvanceRequestService {

    private final AdvanceRequestRepository advanceRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AdvanceRequestService(AdvanceRequestRepository advanceRequestRepository,
                                  ApprovalHistoryRepository approvalHistoryRepository,
                                  AuditService auditService,
                                  NotificationService notificationService) {
        this.advanceRequestRepository = advanceRequestRepository;
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public Optional<AdvanceRequest> findById(Long id) {
        return advanceRequestRepository.findById(id);
    }

    public List<AdvanceRequest> findAll() {
        return advanceRequestRepository.findAll();
    }

    public List<AdvanceRequest> findByEmployee(Long employeeId) {
        return advanceRequestRepository.findByEmployeeId(employeeId);
    }

    public Page<AdvanceRequest> findByEmployee(Long employeeId, Pageable pageable) {
        return advanceRequestRepository.findByEmployeeId(employeeId, pageable);
    }

    public List<AdvanceRequest> findPendingForManager(Long managerId) {
        return advanceRequestRepository.findByManagerIdAndStatus(managerId, AdvanceRequestStatus.PENDING_APPROVAL);
    }

    public Page<AdvanceRequest> findByManager(Long managerId, Pageable pageable) {
        return advanceRequestRepository.findByManagerId(managerId, pageable);
    }

    public List<AdvanceRequest> findByStatus(AdvanceRequestStatus status) {
        return advanceRequestRepository.findByStatus(status);
    }

    @Transactional
    public AdvanceRequest create(AdvanceRequest request) {
        request.setStatus(AdvanceRequestStatus.DRAFT);
        AdvanceRequest saved = advanceRequestRepository.save(request);
        auditService.log("ADVANCE_CREATED", "AdvanceRequest", saved.getId(),
                "Advance request created for ₹" + saved.getRequestedAmount());
        return saved;
    }

    @Transactional
    public AdvanceRequest submit(Long requestId) {
        AdvanceRequest ar = advanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Advance request not found: " + requestId));

        if (ar.getStatus() != AdvanceRequestStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT requests can be submitted");
        }

        ar.setStatus(AdvanceRequestStatus.PENDING_APPROVAL);
        AdvanceRequest saved = advanceRequestRepository.save(ar);

        // Notify manager
        Employee manager = ar.getEmployee().getManager();
        if (manager != null) {
            notificationService.send(manager,
                    "New advance request from " + ar.getEmployee().getFullName() + " for ₹" + ar.getRequestedAmount(),
                    NotificationType.ADVANCE_SUBMITTED, saved.getId(), "AdvanceRequest");
        }

        auditService.log("ADVANCE_SUBMITTED", "AdvanceRequest", saved.getId(),
                "Advance request submitted for approval");
        return saved;
    }

    @Transactional
    public AdvanceRequest approve(Long requestId, Employee approver, String comments) {
        AdvanceRequest ar = advanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Advance request not found: " + requestId));

        if (ar.getStatus() != AdvanceRequestStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Only PENDING_APPROVAL requests can be approved");
        }

        // Prevent self-approval
        if (ar.getEmployee().getId().equals(approver.getId())) {
            throw new RuntimeException("Cannot approve your own request");
        }

        ar.setStatus(AdvanceRequestStatus.APPROVED);
        advanceRequestRepository.save(ar);

        ApprovalHistory history = new ApprovalHistory();
        history.setAdvanceRequest(ar);
        history.setApprover(approver);
        history.setDecision(ApprovalDecision.APPROVED);
        history.setComments(comments);
        approvalHistoryRepository.save(history);

        // Notify employee
        notificationService.send(ar.getEmployee(),
                "Your advance request #" + ar.getId() + " for ₹" + ar.getRequestedAmount() + " has been approved",
                NotificationType.ADVANCE_APPROVED, ar.getId(), "AdvanceRequest");

        auditService.log("ADVANCE_APPROVED", "AdvanceRequest", ar.getId(),
                "Approved by " + approver.getFullName() + ". Comments: " + comments);
        return ar;
    }

    @Transactional
    public AdvanceRequest reject(Long requestId, Employee approver, String comments) {
        AdvanceRequest ar = advanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Advance request not found: " + requestId));

        if (ar.getStatus() != AdvanceRequestStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Only PENDING_APPROVAL requests can be rejected");
        }

        ar.setStatus(AdvanceRequestStatus.REJECTED);
        advanceRequestRepository.save(ar);

        ApprovalHistory history = new ApprovalHistory();
        history.setAdvanceRequest(ar);
        history.setApprover(approver);
        history.setDecision(ApprovalDecision.REJECTED);
        history.setComments(comments);
        approvalHistoryRepository.save(history);

        notificationService.send(ar.getEmployee(),
                "Your advance request #" + ar.getId() + " for ₹" + ar.getRequestedAmount() + " has been rejected. Reason: " + comments,
                NotificationType.ADVANCE_REJECTED, ar.getId(), "AdvanceRequest");

        auditService.log("ADVANCE_REJECTED", "AdvanceRequest", ar.getId(),
                "Rejected by " + approver.getFullName() + ". Reason: " + comments);
        return ar;
    }

    public List<ApprovalHistory> getApprovalHistory(Long requestId) {
        return approvalHistoryRepository.findByAdvanceRequestIdOrderByCreatedAtDesc(requestId);
    }

    public long countByStatus(AdvanceRequestStatus status) {
        return advanceRequestRepository.countByStatus(status);
    }

    public long countByEmployee(Long employeeId) {
        return advanceRequestRepository.countByEmployeeId(employeeId);
    }

    public long countByEmployeeAndStatus(Long employeeId, AdvanceRequestStatus status) {
        return advanceRequestRepository.countByEmployeeIdAndStatus(employeeId, status);
    }

    public BigDecimal sumRequestedByEmployee(Long employeeId) {
        return advanceRequestRepository.sumRequestedAmountByEmployee(employeeId);
    }
}
