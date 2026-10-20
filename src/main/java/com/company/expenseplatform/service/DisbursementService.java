package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.*;
import com.company.expenseplatform.entity.enums.*;
import com.company.expenseplatform.repository.AdvanceDisbursementRepository;
import com.company.expenseplatform.repository.AdvanceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DisbursementService {

    private final AdvanceDisbursementRepository disbursementRepository;
    private final AdvanceRequestRepository advanceRequestRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public DisbursementService(AdvanceDisbursementRepository disbursementRepository,
                               AdvanceRequestRepository advanceRequestRepository,
                               AuditService auditService,
                               NotificationService notificationService) {
        this.disbursementRepository = disbursementRepository;
        this.advanceRequestRepository = advanceRequestRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public List<AdvanceDisbursement> findByAdvanceRequest(Long arId) {
        return disbursementRepository.findByAdvanceRequestId(arId);
    }

    public List<AdvanceDisbursement> findAll() {
        return disbursementRepository.findAll();
    }

    public BigDecimal sumDisbursedForAdvance(Long arId) {
        return disbursementRepository.sumDisbursedByAdvanceRequest(arId);
    }

    @Transactional
    public AdvanceDisbursement disburse(AdvanceDisbursement disbursement) {
        AdvanceRequest ar = disbursement.getAdvanceRequest();

        if (ar.getStatus() != AdvanceRequestStatus.APPROVED && ar.getStatus() != AdvanceRequestStatus.DISBURSED) {
            throw new RuntimeException("Advance must be APPROVED before disbursement");
        }

        // Check for duplicate payment reference
        if (disbursement.getPaymentReference() != null &&
                disbursementRepository.existsByPaymentReference(disbursement.getPaymentReference())) {
            throw new RuntimeException("Duplicate payment reference: " + disbursement.getPaymentReference());
        }

        // Prevent over-disbursement
        BigDecimal alreadyDisbursed = disbursementRepository.sumDisbursedByAdvanceRequest(ar.getId());
        BigDecimal totalAfter = alreadyDisbursed.add(disbursement.getDisbursedAmount());
        if (totalAfter.compareTo(ar.getRequestedAmount()) > 0) {
            throw new RuntimeException("Total disbursement (₹" + totalAfter +
                    ") would exceed requested amount (₹" + ar.getRequestedAmount() + ")");
        }

        AdvanceDisbursement saved = disbursementRepository.save(disbursement);

        // Update advance status to DISBURSED
        ar.setStatus(AdvanceRequestStatus.DISBURSED);
        advanceRequestRepository.save(ar);

        // Notify employee
        notificationService.send(ar.getEmployee(),
                "₹" + disbursement.getDisbursedAmount() + " has been disbursed for advance #" + ar.getId(),
                NotificationType.DISBURSEMENT_COMPLETED, ar.getId(), "AdvanceRequest");

        auditService.log("ADVANCE_DISBURSED", "AdvanceDisbursement", saved.getId(),
                "Disbursed ₹" + saved.getDisbursedAmount() + " via " + saved.getPaymentMethod() +
                        " for advance #" + ar.getId());
        return saved;
    }

    public BigDecimal sumTotalDisbursed() {
        return disbursementRepository.sumTotalDisbursed();
    }
}
