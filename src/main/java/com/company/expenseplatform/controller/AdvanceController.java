package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.AdvanceRequest;
import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.enums.AdvanceRequestStatus;
import com.company.expenseplatform.security.CustomUserDetails;
import com.company.expenseplatform.service.AdvanceRequestService;
import com.company.expenseplatform.service.EmployeeService;
import com.company.expenseplatform.service.PolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/advances")
public class AdvanceController {

    private final AdvanceRequestService advanceService;
    private final EmployeeService employeeService;
    private final PolicyService policyService;

    public AdvanceController(AdvanceRequestService advanceService,
                             EmployeeService employeeService,
                             PolicyService policyService) {
        this.advanceService = advanceService;
        this.employeeService = employeeService;
        this.policyService = policyService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if ("FINANCE_ADMIN".equals(user.getRole())) {
            model.addAttribute("advances", advanceService.findAll());
        } else {
            model.addAttribute("advances", advanceService.findByEmployee(user.getId()));
        }
        return "advances/list";
    }

    @GetMapping("/create")
    public String createForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("advance", new AdvanceRequest());
        model.addAttribute("policies", policyService.findActive());
        Employee emp = employeeService.findById(user.getId()).orElse(null);
        model.addAttribute("employee", emp);
        return "advances/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AdvanceRequest advance,
                       @AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(defaultValue = "false") boolean submitNow,
                       RedirectAttributes redirectAttributes) {
        try {
            Employee emp = employeeService.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            advance.setEmployee(emp);
            advance.setDepartment(emp.getDepartment());

            AdvanceRequest saved = advanceService.create(advance);

            if (submitNow) {
                advanceService.submit(saved.getId());
                redirectAttributes.addFlashAttribute("success", "Advance request submitted for approval");
            } else {
                redirectAttributes.addFlashAttribute("success", "Advance request saved as draft");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/advances";
    }

    @PostMapping("/submit/{id}")
    public String submit(@PathVariable Long id,
                         @AuthenticationPrincipal CustomUserDetails user,
                         RedirectAttributes redirectAttributes) {
        try {
            AdvanceRequest ar = advanceService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Not found"));
            if (!ar.getEmployee().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized");
            }
            advanceService.submit(id);
            redirectAttributes.addFlashAttribute("success", "Advance request submitted");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/advances";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails user,
                       Model model) {
        AdvanceRequest ar = advanceService.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        // Access control: owner, their manager, or finance admin
        boolean isOwner = ar.getEmployee().getId().equals(user.getId());
        boolean isManager = ar.getEmployee().getManager() != null &&
                ar.getEmployee().getManager().getId().equals(user.getId());
        boolean isFinance = "FINANCE_ADMIN".equals(user.getRole());

        if (!isOwner && !isManager && !isFinance) {
            throw new RuntimeException("Unauthorized access");
        }

        model.addAttribute("advance", ar);
        model.addAttribute("approvalHistory", advanceService.getApprovalHistory(id));
        return "advances/view";
    }
}
