package com.company.expenseplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expense_policies")
@Getter
@Setter
@NoArgsConstructor
public class ExpensePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String categoryName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal dailyLimit;

    @Column(nullable = false)
    private boolean requiresReceipt = true;

    @Column(length = 200)
    private String eligibleEmployeeTypes;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(precision = 15, scale = 2)
    private BigDecimal approvalThreshold;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getCategory() {
        return this.categoryName;
    }

    public void setCategory(String category) {
        this.categoryName = category;
    }

    public BigDecimal getMaxLimit() {
        return this.maxAmount;
    }

    public void setMaxLimit(BigDecimal maxLimit) {
        this.maxAmount = maxLimit;
    }

    public boolean isReceiptRequired() {
        return this.requiresReceipt;
    }

    public void setReceiptRequired(boolean receiptRequired) {
        this.requiresReceipt = receiptRequired;
    }
}
