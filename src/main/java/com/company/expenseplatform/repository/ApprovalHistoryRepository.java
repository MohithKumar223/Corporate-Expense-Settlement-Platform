package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {

    List<ApprovalHistory> findByAdvanceRequestIdOrderByCreatedAtDesc(Long advanceRequestId);

    List<ApprovalHistory> findByApproverId(Long approverId);

    long countByApproverId(Long approverId);
}
