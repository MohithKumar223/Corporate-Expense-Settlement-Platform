package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.*;
import com.company.expenseplatform.entity.enums.AdvanceRequestStatus;
import com.company.expenseplatform.security.CustomUserDetails;
import com.company.expenseplatform.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/disbursements")
@PreAuthorize("hasRole('FINANCE_ADMIN')")
public class DisbursementController {

    private final DisbursementService disbursementService;
    private final AdvanceRequestService advanceService;
    private final EmployeeService employeeService;

    public DisbursementController(DisbursementService disbursementService,
                                   AdvanceRequestService advanceService,
                                   EmployeeService employeeService) {
        this.disbursementService = disbursementService;
        this.advanceService = advanceService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("disbursements", disbursementService.findAll());
        model.addAttribute("approvedAdvances", advanceService.findByStatus(AdvanceRequestStatus.APPROVED));
        return "disbursements/list";
    }

    @GetMapping("/create/{advanceId}")
    public String createForm(@PathVariable Long advanceId, Model model) {
        AdvanceRequest ar = advanceService.findById(advanceId)
                .orElseThrow(() -> new RuntimeException("Advance not found"));
        model.addAttribute("advance", ar);
        model.addAttribute("disbursement", new AdvanceDisbursement());
        model.addAttribute("alreadyDisbursed", disbursementService.sumDisbursedForAdvance(advanceId));
        return "disbursements/form";
    }

    @PostMapping("/save")
    public String save(@RequestParam Long advanceRequestId,
                       @ModelAttribute AdvanceDisbursement disbursement,
                       @AuthenticationPrincipal CustomUserDetails user,
                       RedirectAttributes redirectAttributes) {
        try {
            AdvanceRequest ar = advanceService.findById(advanceRequestId)
                    .orElseThrow(() -> new RuntimeException("Advance not found"));
            Employee financeUser = employeeService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            disbursement.setAdvanceRequest(ar);
            disbursement.setDisbursedBy(financeUser);
            if (disbursement.getPaymentDate() == null) {
                disbursement.setPaymentDate(LocalDate.now());
            }

            disbursementService.disburse(disbursement);
            redirectAttributes.addFlashAttribute("success", "Disbursement recorded successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/disbursements";
    }
}
