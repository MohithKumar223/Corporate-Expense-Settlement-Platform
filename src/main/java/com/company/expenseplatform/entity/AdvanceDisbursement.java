package com.company.expenseplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "advance_disbursements")
@Getter
@Setter
@NoArgsConstructor
public class AdvanceDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "advance_request_id", nullable = false)
    private AdvanceRequest advanceRequest;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal disbursedAmount;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(length = 100)
    private String paymentReference;

    @Column(length = 50)
    private String paymentMethod;

    @Column(length = 500)
    private String financeRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursed_by")
    private Employee disbursedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public BigDecimal getAmountDisbursed() {
        return disbursedAmount;
    }

    public void setAmountDisbursed(BigDecimal amountDisbursed) {
        this.disbursedAmount = amountDisbursed;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getTransactionReference() {
        return paymentReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.paymentReference = transactionReference;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getFinanceRemarks() {
        return financeRemarks;
    }

    public void setFinanceRemarks(String financeRemarks) {
        this.financeRemarks = financeRemarks;
    }

    public String getRemarks() {
        return financeRemarks;
    }

    public void setRemarks(String remarks) {
        this.financeRemarks = remarks;
    }

    public Employee getDisbursedBy() {
        return disbursedBy;
    }

    public void setDisbursedBy(Employee disbursedBy) {
        this.disbursedBy = disbursedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
