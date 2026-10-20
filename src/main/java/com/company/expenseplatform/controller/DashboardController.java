package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.enums.*;
import com.company.expenseplatform.security.CustomUserDetails;
import com.company.expenseplatform.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;

@Controller
public class DashboardController {

    private final AdvanceRequestService advanceService;
    private final ExpenseService expenseService;
    private final SettlementService settlementService;
    private final DisbursementService disbursementService;
    private final NotificationService notificationService;
    private final EmployeeService employeeService;
    private final ExceptionService exceptionService;

    public DashboardController(AdvanceRequestService advanceService,
                               ExpenseService expenseService,
                               SettlementService settlementService,
                               DisbursementService disbursementService,
                               NotificationService notificationService,
                               EmployeeService employeeService,
                               ExceptionService exceptionService) {
        this.advanceService = advanceService;
        this.expenseService = expenseService;
        this.settlementService = settlementService;
        this.disbursementService = disbursementService;
        this.notificationService = notificationService;
        this.employeeService = employeeService;
        this.exceptionService = exceptionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("currentUser", user);
        long unreadNotifications = notificationService.countUnread(user.getId());
        model.addAttribute("unreadNotifications", unreadNotifications);

        String role = user.getRole();

        if ("FINANCE_ADMIN".equals(role)) {
            return financeDashboard(user, model);
        } else if ("MANAGER".equals(role)) {
            return managerDashboard(user, model);
        } else {
            return employeeDashboard(user, model);
        }
    }

    private String employeeDashboard(CustomUserDetails user, Model model) {
        Long empId = user.getId();

        model.addAttribute("totalAdvances", advanceService.countByEmployee(empId));
        model.addAttribute("approvedAdvances", advanceService.countByEmployeeAndStatus(empId, AdvanceRequestStatus.APPROVED));
        model.addAttribute("totalExpenses", expenseService.countByEmployee(empId));
        model.addAttribute("pendingApprovals", advanceService.countByEmployeeAndStatus(empId, AdvanceRequestStatus.PENDING_APPROVAL));
        model.addAttribute("totalRequested", advanceService.sumRequestedByEmployee(empId));

        // Recent advances
        model.addAttribute("recentAdvances", advanceService.findByEmployee(empId));
        // Recent expenses
        model.addAttribute("recentExpenses", expenseService.findByEmployee(empId));
        // Settlements
        model.addAttribute("settlements", settlementService.findByEmployee(empId));
        // Notifications
        model.addAttribute("notifications", notificationService.getUnread(empId));

        return "dashboard/employee";
    }

    private String managerDashboard(CustomUserDetails user, Model model) {
        Long mgrId = user.getId();

        // Manager's own data
        model.addAttribute("totalAdvances", advanceService.countByEmployee(mgrId));
        model.addAttribute("totalExpenses", expenseService.countByEmployee(mgrId));
        model.addAttribute("totalRequested", advanceService.sumRequestedByEmployee(mgrId));

        // Pending approvals for subordinates
        model.addAttribute("pendingApprovals", advanceService.findPendingForManager(mgrId));
        model.addAttribute("pendingCount", advanceService.findPendingForManager(mgrId).size());

        // Subordinates
        model.addAttribute("subordinates", employeeService.findSubordinates(mgrId));

        model.addAttribute("notifications", notificationService.getUnread(mgrId));

        return "dashboard/manager";
    }

    private String financeDashboard(CustomUserDetails user, Model model) {
        // Advance stats
        model.addAttribute("totalAdvanceRequests", advanceService.findAll().size());
        model.addAttribute("pendingApprovals", advanceService.countByStatus(AdvanceRequestStatus.PENDING_APPROVAL));
        model.addAttribute("approvedAdvances", advanceService.countByStatus(AdvanceRequestStatus.APPROVED));
        model.addAttribute("disbursedAdvances", advanceService.countByStatus(AdvanceRequestStatus.DISBURSED));

        // Financial totals
        model.addAttribute("totalDisbursed", disbursementService.sumTotalDisbursed());
        model.addAttribute("totalValidatedExpenses", expenseService.sumTotalValidated());
        model.addAttribute("pendingRecovery", settlementService.sumPendingRecovery());
        model.addAttribute("pendingReimbursement", settlementService.sumPendingReimbursement());

        // Settlement stats
        model.addAttribute("pendingSettlements", settlementService.countByStatus(SettlementStatus.PENDING_RECOVERY)
                + settlementService.countByStatus(SettlementStatus.PENDING_REIMBURSEMENT)
                + settlementService.countByStatus(SettlementStatus.CALCULATED));
        model.addAttribute("completedSettlements", settlementService.countByStatus(SettlementStatus.COMPLETED));

        // Exceptions
        model.addAttribute("openExceptions", exceptionService.countOpen());

        // Employee count
        model.addAttribute("totalEmployees", employeeService.countActive());

        // Charts data
        model.addAttribute("expensesByDepartment", expenseService.sumByDepartment());
        model.addAttribute("expensesByMonth", expenseService.sumByMonth());
        model.addAttribute("expensesByCategory", expenseService.sumByCategory());

        model.addAttribute("notifications", notificationService.getUnread(user.getId()));

        return "dashboard/finance";
    }
}
