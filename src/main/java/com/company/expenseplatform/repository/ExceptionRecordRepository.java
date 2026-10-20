package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.ExceptionRecord;
import com.company.expenseplatform.entity.enums.ExceptionStatus;
import com.company.expenseplatform.entity.enums.ExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExceptionRecordRepository extends JpaRepository<ExceptionRecord, Long> {

    List<ExceptionRecord> findByStatus(ExceptionStatus status);

    List<ExceptionRecord> findByExpenseId(Long expenseId);

    List<ExceptionRecord> findByAdvanceRequestId(Long advanceRequestId);

    List<ExceptionRecord> findByType(ExceptionType type);

    long countByStatus(ExceptionStatus status);

    boolean existsByExpenseIdAndTypeAndStatusNot(Long expenseId, ExceptionType type, ExceptionStatus status);
}
