package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.AdvanceRequest;
import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.security.CustomUserDetails;
import com.company.expenseplatform.service.AdvanceRequestService;
import com.company.expenseplatform.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/approvals")
@PreAuthorize("hasAnyRole('MANAGER','FINANCE_ADMIN')")
public class ApprovalController {

    private final AdvanceRequestService advanceService;
    private final EmployeeService employeeService;

    public ApprovalController(AdvanceRequestService advanceService, EmployeeService employeeService) {
        this.advanceService = advanceService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if ("FINANCE_ADMIN".equals(user.getRole())) {
            model.addAttribute("pendingRequests", advanceService.findByStatus(
                    com.company.expenseplatform.entity.enums.AdvanceRequestStatus.PENDING_APPROVAL));
        } else {
            model.addAttribute("pendingRequests", advanceService.findPendingForManager(user.getId()));
        }
        return "approvals/list";
    }

    @GetMapping("/review/{id}")
    public String review(@PathVariable Long id, Model model) {
        AdvanceRequest ar = advanceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        model.addAttribute("advance", ar);
        model.addAttribute("approvalHistory", advanceService.getApprovalHistory(id));
        return "approvals/review";
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) String comments,
                          @AuthenticationPrincipal CustomUserDetails user,
                          RedirectAttributes redirectAttributes) {
        try {
            Employee approver = employeeService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Approver not found"));
            advanceService.approve(id, approver, comments);
            redirectAttributes.addFlashAttribute("success", "Request approved successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }

    @PostMapping("/reject/{id}")
    public String reject(@PathVariable Long id,
                         @RequestParam String comments,
                         @AuthenticationPrincipal CustomUserDetails user,
                         RedirectAttributes redirectAttributes) {
        try {
            Employee approver = employeeService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Approver not found"));
            advanceService.reject(id, approver, comments);
            redirectAttributes.addFlashAttribute("success", "Request rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/approvals";
    }
}
