package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.AdvanceDisbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdvanceDisbursementRepository extends JpaRepository<AdvanceDisbursement, Long> {

    List<AdvanceDisbursement> findByAdvanceRequestId(Long advanceRequestId);

    @Query("SELECT COALESCE(SUM(d.disbursedAmount), 0) FROM AdvanceDisbursement d WHERE d.advanceRequest.id = :arId")
    BigDecimal sumDisbursedByAdvanceRequest(@Param("arId") Long advanceRequestId);

    @Query("SELECT COALESCE(SUM(d.disbursedAmount), 0) FROM AdvanceDisbursement d")
    BigDecimal sumTotalDisbursed();

    boolean existsByPaymentReference(String paymentReference);

    long countByAdvanceRequestId(Long advanceRequestId);
}
