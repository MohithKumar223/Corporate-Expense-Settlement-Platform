package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.Expense;
import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.AdvanceRequest;
import com.company.expenseplatform.security.CustomUserDetails;
import com.company.expenseplatform.service.AdvanceRequestService;
import com.company.expenseplatform.service.EmployeeService;
import com.company.expenseplatform.service.ExpenseService;
import com.company.expenseplatform.service.PolicyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final AdvanceRequestService advanceService;
    private final EmployeeService employeeService;
    private final PolicyService policyService;

    public ExpenseController(ExpenseService expenseService,
                             AdvanceRequestService advanceService,
                             EmployeeService employeeService,
                             PolicyService policyService) {
        this.expenseService = expenseService;
        this.advanceService = advanceService;
        this.employeeService = employeeService;
        this.policyService = policyService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if ("FINANCE_ADMIN".equals(user.getRole())) {
            model.addAttribute("expenses", expenseService.findAll());
        } else {
            model.addAttribute("expenses", expenseService.findByEmployee(user.getId()));
        }
        return "expenses/list";
    }

    @GetMapping("/create")
    public String createForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("expense", new Expense());
        model.addAttribute("policies", policyService.findActive());
        // Show advances that are disbursed for the employee
        model.addAttribute("advances", advanceService.findByEmployee(user.getId()));
        return "expenses/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Expense expense,
                       @RequestParam(required = false) Long advanceRequestId,
                       @RequestParam(required = false) MultipartFile receipt,
                       @AuthenticationPrincipal CustomUserDetails user,
                       RedirectAttributes redirectAttributes) {
        try {
            Employee emp = employeeService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            expense.setEmployee(emp);

            if (advanceRequestId != null) {
                AdvanceRequest ar = advanceService.findById(advanceRequestId)
                        .orElseThrow(() -> new RuntimeException("Advance request not found"));
                expense.setAdvanceRequest(ar);
            }

            List<String> warnings = expenseService.submitExpense(expense, receipt);

            if (!warnings.isEmpty()) {
                redirectAttributes.addFlashAttribute("warnings", warnings);
                redirectAttributes.addFlashAttribute("success", "Expense submitted with warnings");
            } else {
                redirectAttributes.addFlashAttribute("success", "Expense submitted successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/expenses/create";
        }
        return "redirect:/expenses";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails user,
                       Model model) {
        Expense expense = expenseService.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        boolean isOwner = expense.getEmployee().getId().equals(user.getId());
        boolean isFinance = "FINANCE_ADMIN".equals(user.getRole());
        if (!isOwner && !isFinance) {
            throw new RuntimeException("Unauthorized");
        }

        model.addAttribute("expense", expense);
        return "expenses/view";
    }

    @PostMapping("/validate/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String validate(@PathVariable Long id,
                           @RequestParam(required = false) String notes,
                           RedirectAttributes redirectAttributes) {
        try {
            expenseService.validate(id, notes);
            redirectAttributes.addFlashAttribute("success", "Expense validated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/expenses";
    }

    @PostMapping("/approve/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            expenseService.approve(id);
            redirectAttributes.addFlashAttribute("success", "Expense approved");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/expenses";
    }

    @PostMapping("/reject/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            expenseService.reject(id, reason);
            redirectAttributes.addFlashAttribute("success", "Expense rejected");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/expenses";
    }

    @PostMapping("/dispute/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String dispute(@PathVariable Long id,
                          @RequestParam String reason,
                          RedirectAttributes redirectAttributes) {
        try {
            expenseService.dispute(id, reason);
            redirectAttributes.addFlashAttribute("success", "Expense disputed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/expenses";
    }
}
