package com.company.expenseplatform.entity;

import com.company.expenseplatform.entity.enums.AdvanceRequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "advance_requests")
@Getter
@Setter
@NoArgsConstructor
public class AdvanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 300)
    private String purpose;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 100)
    private String department;

    @Column(length = 200)
    private String businessActivity;

    @Column(length = 500)
    private String supportingInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AdvanceRequestStatus status = AdvanceRequestStatus.DRAFT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ─── Related entities ───────────────────────────────────────────────────────

    @OneToMany(mappedBy = "advanceRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ApprovalHistory> approvalHistory = new ArrayList<>();

    @OneToMany(mappedBy = "advanceRequest", fetch = FetchType.LAZY)
    private List<AdvanceDisbursement> disbursements = new ArrayList<>();

    @OneToMany(mappedBy = "advanceRequest", fetch = FetchType.LAZY)
    private List<Expense> expenses = new ArrayList<>();

    @OneToOne(mappedBy = "advanceRequest", fetch = FetchType.LAZY)
    private Settlement settlement;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.category == null || this.category.isBlank()) {
            this.category = "Travel";
        }
        if (this.startDate == null) {
            this.startDate = LocalDate.now().plusDays(3);
        }
        if (this.endDate == null) {
            this.endDate = this.startDate.plusDays(5);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getRequestNumber() {
        return "ADV-" + (id != null ? String.format("%04d", id) : "NEW");
    }

    public LocalDate getNeededByDate() {
        return startDate != null ? startDate : (createdAt != null ? createdAt.toLocalDate() : LocalDate.now());
    }

    public void setNeededByDate(LocalDate date) {
        this.startDate = date;
    }

    public LocalDate getReturnDate() {
        return endDate;
    }

    public void setReturnDate(LocalDate date) {
        this.endDate = date;
    }

    public String getJustification() {
        return supportingInfo;
    }

    public void setJustification(String justification) {
        this.supportingInfo = justification;
    }

    public LocalDate getRequestDate() {
        return createdAt != null ? createdAt.toLocalDate() : (startDate != null ? startDate : LocalDate.now());
    }

    public BigDecimal getDisbursedAmount() {
        if (disbursements != null && !disbursements.isEmpty()) {
            return disbursements.stream()
                    .map(AdvanceDisbursement::getDisbursedAmount)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBusinessActivity() {
        return businessActivity;
    }

    public void setBusinessActivity(String businessActivity) {
        this.businessActivity = businessActivity;
    }

    public String getSupportingInfo() {
        return supportingInfo;
    }

    public void setSupportingInfo(String supportingInfo) {
        this.supportingInfo = supportingInfo;
    }

    public AdvanceRequestStatus getStatus() {
        return status;
    }

    public void setStatus(AdvanceRequestStatus status) {
        this.status = status;
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

    public List<ApprovalHistory> getApprovalHistory() {
        return approvalHistory;
    }

    public void setApprovalHistory(List<ApprovalHistory> approvalHistory) {
        this.approvalHistory = approvalHistory;
    }

    public List<AdvanceDisbursement> getDisbursements() {
        return disbursements;
    }

    public void setDisbursements(List<AdvanceDisbursement> disbursements) {
        this.disbursements = disbursements;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
    }

    public Settlement getSettlement() {
        return settlement;
    }

    public void setSettlement(Settlement settlement) {
        this.settlement = settlement;
    }
}
