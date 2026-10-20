package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.AdvanceRequest;
import com.company.expenseplatform.entity.enums.AdvanceRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AdvanceRequestRepository extends JpaRepository<AdvanceRequest, Long> {

    List<AdvanceRequest> findByEmployeeId(Long employeeId);

    Page<AdvanceRequest> findByEmployeeId(Long employeeId, Pageable pageable);

    List<AdvanceRequest> findByEmployeeIdAndStatus(Long employeeId, AdvanceRequestStatus status);

    List<AdvanceRequest> findByStatus(AdvanceRequestStatus status);

    @Query("SELECT ar FROM AdvanceRequest ar JOIN FETCH ar.employee WHERE ar.employee.manager.id = :managerId AND ar.status = :status")
    List<AdvanceRequest> findByManagerIdAndStatus(@Param("managerId") Long managerId,
                                                   @Param("status") AdvanceRequestStatus status);

    @Query("SELECT ar FROM AdvanceRequest ar WHERE ar.employee.manager.id = :managerId")
    Page<AdvanceRequest> findByManagerId(@Param("managerId") Long managerId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(ar.requestedAmount), 0) FROM AdvanceRequest ar WHERE ar.employee.id = :empId AND ar.status NOT IN (com.company.expenseplatform.entity.enums.AdvanceRequestStatus.REJECTED, com.company.expenseplatform.entity.enums.AdvanceRequestStatus.CANCELLED)")
    BigDecimal sumRequestedAmountByEmployee(@Param("empId") Long employeeId);

    @Query("SELECT COALESCE(SUM(ar.requestedAmount), 0) FROM AdvanceRequest ar WHERE ar.status = :status")
    BigDecimal sumRequestedAmountByStatus(@Param("status") AdvanceRequestStatus status);

    long countByStatus(AdvanceRequestStatus status);

    long countByEmployeeId(Long employeeId);

    long countByEmployeeIdAndStatus(Long employeeId, AdvanceRequestStatus status);

    @Query("SELECT ar FROM AdvanceRequest ar WHERE ar.employee.department = :dept")
    List<AdvanceRequest> findByDepartment(@Param("dept") String department);

    @Query("SELECT ar FROM AdvanceRequest ar WHERE ar.status IN :statuses")
    List<AdvanceRequest> findByStatusIn(@Param("statuses") List<AdvanceRequestStatus> statuses);
}
