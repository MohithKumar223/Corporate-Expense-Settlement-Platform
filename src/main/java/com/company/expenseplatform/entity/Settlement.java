package com.company.expenseplatform.entity;

import com.company.expenseplatform.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advance_request_id", nullable = false, unique = true)
    private AdvanceRequest advanceRequest;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDisbursed = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalValidatedExpenses = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal settlementDifference = BigDecimal.ZERO;

    /** Positive when employee must return unused advance */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal recoveryAmount = BigDecimal.ZERO;

    /** Positive when company owes additional reimbursement */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal reimbursementAmount = BigDecimal.ZERO;

    /** Total amount already recovered from the employee */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRecovered = BigDecimal.ZERO;

    /** Total amount already reimbursed to the employee */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalReimbursed = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private SettlementStatus status = SettlementStatus.PENDING_CALCULATION;

    private LocalDateTime calculatedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "settlement", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<RecoveryTransaction> recoveryTransactions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AdvanceRequest getAdvanceRequest() {
        return advanceRequest;
    }

    public void setAdvanceRequest(AdvanceRequest advanceRequest) {
        this.advanceRequest = advanceRequest;
    }

    public BigDecimal getTotalDisbursed() {
        return totalDisbursed;
    }

    public void setTotalDisbursed(BigDecimal totalDisbursed) {
        this.totalDisbursed = totalDisbursed;
    }

    public BigDecimal getTotalValidatedExpenses() {
        return totalValidatedExpenses;
    }

    public void setTotalValidatedExpenses(BigDecimal totalValidatedExpenses) {
        this.totalValidatedExpenses = totalValidatedExpenses;
    }

    public BigDecimal getSettlementDifference() {
        return settlementDifference;
    }

    public void setSettlementDifference(BigDecimal settlementDifference) {
        this.settlementDifference = settlementDifference;
    }

    public BigDecimal getRecoveryAmount() {
        return recoveryAmount;
    }

    public void setRecoveryAmount(BigDecimal recoveryAmount) {
        this.recoveryAmount = recoveryAmount;
    }

    public BigDecimal getReimbursementAmount() {
        return reimbursementAmount;
    }

    public void setReimbursementAmount(BigDecimal reimbursementAmount) {
        this.reimbursementAmount = reimbursementAmount;
    }

    public BigDecimal getTotalRecovered() {
        return totalRecovered;
    }

    public void setTotalRecovered(BigDecimal totalRecovered) {
        this.totalRecovered = totalRecovered;
    }

    public BigDecimal getTotalReimbursed() {
        return totalReimbursed;
    }

    public void setTotalReimbursed(BigDecimal totalReimbursed) {
        this.totalReimbursed = totalReimbursed;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<RecoveryTransaction> getRecoveryTransactions() {
        return recoveryTransactions;
    }

    public void setRecoveryTransactions(List<RecoveryTransaction> recoveryTransactions) {
        this.recoveryTransactions = recoveryTransactions;
    }
}
