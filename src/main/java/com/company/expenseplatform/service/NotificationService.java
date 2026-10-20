package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.Notification;
import com.company.expenseplatform.entity.enums.NotificationType;
import com.company.expenseplatform.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void send(Employee employee, String message, NotificationType type, Long referenceId, String referenceType) {
        Notification n = new Notification();
        n.setEmployee(employee);
        n.setMessage(message);
        n.setType(type);
        n.setReferenceId(referenceId);
        n.setReferenceType(referenceType);
        notificationRepository.save(n);
    }

    public List<Notification> getUnread(Long employeeId) {
        return notificationRepository.findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(employeeId);
    }

    public Page<Notification> getAll(Long employeeId, Pageable pageable) {
        return notificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId, pageable);
    }

    public long countUnread(Long employeeId) {
        return notificationRepository.countByEmployeeIdAndReadFalse(employeeId);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead(Long employeeId) {
        List<Notification> unread = notificationRepository.findByEmployeeIdAndReadFalseOrderByCreatedAtDesc(employeeId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }
}
