package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.Expense;
import com.company.expenseplatform.entity.enums.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByEmployeeId(Long employeeId);

    Page<Expense> findByEmployeeId(Long employeeId, Pageable pageable);

    List<Expense> findByAdvanceRequestId(Long advanceRequestId);

    List<Expense> findByAdvanceRequestIdAndStatusIn(Long advanceRequestId, List<ExpenseStatus> statuses);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.advanceRequest.id = :arId AND e.status IN :statuses")
    BigDecimal sumAmountByAdvanceRequestAndStatuses(@Param("arId") Long advanceRequestId,
                                                    @Param("statuses") List<ExpenseStatus> statuses);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.employee.id = :empId AND e.expenseDate = :date AND e.category = :cat")
    BigDecimal sumDailyExpenseByCategory(@Param("empId") Long employeeId,
                                         @Param("date") LocalDate date,
                                         @Param("cat") String category);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status IN :statuses")
    BigDecimal sumTotalByStatuses(@Param("statuses") List<ExpenseStatus> statuses);

    long countByStatus(ExpenseStatus status);

    long countByEmployeeId(Long employeeId);

    long countByEmployeeIdAndStatus(Long employeeId, ExpenseStatus status);

    @Query("SELECT e FROM Expense e WHERE e.employee.id = :empId AND e.category = :cat AND e.expenseDate = :date AND e.amount = :amt AND e.status != com.company.expenseplatform.entity.enums.ExpenseStatus.REJECTED")
    List<Expense> findPotentialDuplicates(@Param("empId") Long employeeId,
                                          @Param("cat") String category,
                                          @Param("date") LocalDate date,
                                          @Param("amt") BigDecimal amount);

    @Query("SELECT DISTINCT e.category FROM Expense e ORDER BY e.category")
    List<String> findDistinctCategories();

    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status IN (com.company.expenseplatform.entity.enums.ExpenseStatus.VALIDATED, com.company.expenseplatform.entity.enums.ExpenseStatus.APPROVED) GROUP BY e.category")
    List<Object[]> sumExpensesByCategory();

    @Query("SELECT e.employee.department, COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.status IN (com.company.expenseplatform.entity.enums.ExpenseStatus.VALIDATED, com.company.expenseplatform.entity.enums.ExpenseStatus.APPROVED) AND e.employee.department IS NOT NULL GROUP BY e.employee.department")
    List<Object[]> sumExpensesByDepartment();

    @Query("SELECT MONTH(e.expenseDate), YEAR(e.expenseDate), COALESCE(SUM(e.amount), 0) " +
           "FROM Expense e WHERE e.status IN (com.company.expenseplatform.entity.enums.ExpenseStatus.VALIDATED, com.company.expenseplatform.entity.enums.ExpenseStatus.APPROVED) GROUP BY YEAR(e.expenseDate), MONTH(e.expenseDate) " +
           "ORDER BY YEAR(e.expenseDate), MONTH(e.expenseDate)")
    List<Object[]> sumExpensesByMonth();
}
