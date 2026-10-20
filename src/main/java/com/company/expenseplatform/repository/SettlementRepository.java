package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.Settlement;
import com.company.expenseplatform.entity.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByAdvanceRequestId(Long advanceRequestId);

    boolean existsByAdvanceRequestId(Long advanceRequestId);

    List<Settlement> findByStatus(SettlementStatus status);

    @Query("SELECT COALESCE(SUM(s.recoveryAmount), 0) FROM Settlement s WHERE s.status IN ('CALCULATED','PENDING_RECOVERY')")
    BigDecimal sumPendingRecoveryAmounts();

    @Query("SELECT COALESCE(SUM(s.reimbursementAmount), 0) FROM Settlement s WHERE s.status IN ('CALCULATED','PENDING_REIMBURSEMENT')")
    BigDecimal sumPendingReimbursementAmounts();

    long countByStatus(SettlementStatus status);

    @Query("SELECT s FROM Settlement s WHERE s.advanceRequest.employee.id = :empId")
    List<Settlement> findByEmployeeId(@Param("empId") Long employeeId);
}
