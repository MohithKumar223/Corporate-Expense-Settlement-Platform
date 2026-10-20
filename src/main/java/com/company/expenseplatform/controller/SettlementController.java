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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/settlements")
public class SettlementController {

    private final SettlementService settlementService;
    private final AdvanceRequestService advanceService;
    private final DisbursementService disbursementService;
    private final ExpenseService expenseService;
    private final EmployeeService employeeService;

    public SettlementController(SettlementService settlementService,
                                AdvanceRequestService advanceService,
                                DisbursementService disbursementService,
                                ExpenseService expenseService,
                                EmployeeService employeeService) {
        this.settlementService = settlementService;
        this.advanceService = advanceService;
        this.disbursementService = disbursementService;
        this.expenseService = expenseService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if ("FINANCE_ADMIN".equals(user.getRole())) {
            model.addAttribute("settlements", settlementService.findAll());
            // Show disbursed advances that need settlement
            model.addAttribute("disbursedAdvances", advanceService.findByStatus(AdvanceRequestStatus.DISBURSED));
        } else {
            model.addAttribute("settlements", settlementService.findByEmployee(user.getId()));
        }
        return "settlements/list";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        Settlement settlement = settlementService.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
        model.addAttribute("settlement", settlement);
        model.addAttribute("recoveries", settlementService.getRecoveryTransactions(id));
        model.addAttribute("expenses", expenseService.findByAdvanceRequest(
                settlement.getAdvanceRequest().getId()));
        return "settlements/view";
    }

    @PostMapping("/calculate/{advanceId}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String calculate(@PathVariable Long advanceId, RedirectAttributes redirectAttributes) {
        try {
            Settlement s = settlementService.calculateSettlement(advanceId);
            redirectAttributes.addFlashAttribute("success",
                    "Settlement calculated. Difference: ₹" + s.getSettlementDifference());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settlements";
    }

    @PostMapping("/recover/{settlementId}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String recordRecovery(@PathVariable Long settlementId,
                                 @RequestParam BigDecimal amount,
                                 @RequestParam(required = false) String paymentReference,
                                 @RequestParam(required = false) String remarks,
                                 @AuthenticationPrincipal CustomUserDetails user,
                                 RedirectAttributes redirectAttributes) {
        try {
            Employee recorder = employeeService.findById(user.getId()).orElse(null);
            RecoveryTransaction recovery = new RecoveryTransaction();
            recovery.setAmountRecovered(amount);
            recovery.setRecoveryDate(LocalDate.now());
            recovery.setPaymentReference(paymentReference);
            recovery.setRemarks(remarks);
            recovery.setRecordedBy(recorder);

            settlementService.recordRecovery(settlementId, recovery);
            redirectAttributes.addFlashAttribute("success", "Recovery of ₹" + amount + " recorded");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settlements/view/" + settlementId;
    }

    @PostMapping("/reimburse/{settlementId}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String recordReimbursement(@PathVariable Long settlementId,
                                      @RequestParam BigDecimal amount,
                                      RedirectAttributes redirectAttributes) {
        try {
            settlementService.recordReimbursement(settlementId, amount);
            redirectAttributes.addFlashAttribute("success", "Reimbursement of ₹" + amount + " recorded");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settlements/view/" + settlementId;
    }
}
