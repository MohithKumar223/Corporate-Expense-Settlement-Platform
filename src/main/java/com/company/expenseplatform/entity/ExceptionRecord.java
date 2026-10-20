package com.company.expenseplatform.entity;

import com.company.expenseplatform.entity.enums.ExceptionStatus;
import com.company.expenseplatform.entity.enums.ExceptionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "exception_records")
@Getter
@Setter
@NoArgsConstructor
public class ExceptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advance_request_id")
    private AdvanceRequest advanceRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 35)
    private ExceptionType type;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ExceptionStatus status = ExceptionStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigated_by")
    private Employee investigatedBy;

    @Column(length = 1000)
    private String resolution;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ExceptionType getExceptionType() {
        return type;
    }

    public String getEntityType() {
        if (expense != null) return "Expense";
        if (advanceRequest != null) return "AdvanceRequest";
        return "General";
    }

    public Long getEntityId() {
        if (expense != null) return expense.getId();
        if (advanceRequest != null) return advanceRequest.getId();
        return null;
    }

    public String getResolutionNotes() {
        return resolution;
    }
}
