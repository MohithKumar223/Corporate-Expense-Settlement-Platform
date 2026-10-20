package com.company.expenseplatform.controller;

import com.company.expenseplatform.security.CustomUserDetails;
import com.company.expenseplatform.service.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ReportController {

    private final ExpenseService expenseService;
    private final AdvanceRequestService advanceService;
    private final SettlementService settlementService;
    private final DisbursementService disbursementService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ReportController(ExpenseService expenseService,
                            AdvanceRequestService advanceService,
                            SettlementService settlementService,
                            DisbursementService disbursementService,
                            AuditService auditService,
                            NotificationService notificationService) {
        this.expenseService = expenseService;
        this.advanceService = advanceService;
        this.settlementService = settlementService;
        this.disbursementService = disbursementService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String analytics(Model model) {
        model.addAttribute("totalDisbursed", disbursementService.sumTotalDisbursed());
        model.addAttribute("totalValidated", expenseService.sumTotalValidated());
        model.addAttribute("pendingRecovery", settlementService.sumPendingRecovery());
        model.addAttribute("pendingReimbursement", settlementService.sumPendingReimbursement());
        model.addAttribute("totalRecovered", settlementService.sumTotalRecovered());

        model.addAttribute("expensesByCategory", expenseService.sumByCategory());
        model.addAttribute("expensesByDepartment", expenseService.sumByDepartment());
        model.addAttribute("expensesByMonth", expenseService.sumByMonth());

        return "reports/analytics";
    }

    @GetMapping("/analytics/export/expenses")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public void exportExpenses(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=expenses_report.csv");
        PrintWriter writer = response.getWriter();
        writer.println("ID,Employee,Category,Date,Amount,Status,Description");
        expenseService.findAll().forEach(e -> {
            writer.println(e.getId() + "," +
                    csvEscape(e.getEmployee().getFullName()) + "," +
                    csvEscape(e.getCategory()) + "," +
                    e.getExpenseDate() + "," +
                    e.getAmount() + "," +
                    e.getStatus() + "," +
                    csvEscape(e.getDescription()));
        });
        writer.flush();
    }

    @GetMapping("/analytics/export/settlements")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public void exportSettlements(HttpServletResponse response) throws Exception {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=settlements_report.csv");
        PrintWriter writer = response.getWriter();
        writer.println("ID,AdvanceID,Employee,TotalDisbursed,TotalExpenses,Difference,Recovery,Reimbursement,Status");
        settlementService.findAll().forEach(s -> {
            writer.println(s.getId() + "," +
                    s.getAdvanceRequest().getId() + "," +
                    csvEscape(s.getAdvanceRequest().getEmployee().getFullName()) + "," +
                    s.getTotalDisbursed() + "," +
                    s.getTotalValidatedExpenses() + "," +
                    s.getSettlementDifference() + "," +
                    s.getRecoveryAmount() + "," +
                    s.getReimbursementAmount() + "," +
                    s.getStatus());
        });
        writer.flush();
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String auditLog(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("auditPage", auditService.getAll(PageRequest.of(page, 50)));
        model.addAttribute("currentPage", page);
        return "audit/list";
    }

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("notifications",
                notificationService.getAll(user.getId(), PageRequest.of(0, 100)));
        return "notifications/list";
    }

    @PostMapping("/notifications/read/{id}")
    public String markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(@AuthenticationPrincipal CustomUserDetails user) {
        notificationService.markAllRead(user.getId());
        return "redirect:/notifications";
    }

    @GetMapping("/error/403")
    public String accessDenied() {
        return "error/403";
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
