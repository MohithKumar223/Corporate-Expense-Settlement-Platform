package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    Page<Notification> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId, Pageable pageable);

    List<Notification> findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(Long employeeId);

    long countByEmployeeIdAndReadFalse(Long employeeId);
}
