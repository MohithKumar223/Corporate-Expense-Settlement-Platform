package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.ExpensePolicy;
import com.company.expenseplatform.service.PolicyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/policies")
@PreAuthorize("hasRole('FINANCE_ADMIN')")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("policies", policyService.findAll());
        return "policies/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("policy", new ExpensePolicy());
        return "policies/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        ExpensePolicy policy = policyService.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found"));
        model.addAttribute("policy", policy);
        return "policies/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute ExpensePolicy policy, RedirectAttributes redirectAttributes) {
        try {
            if (policy.getId() == null) {
                policyService.save(policy);
                redirectAttributes.addFlashAttribute("success", "Policy created successfully");
            } else {
                policyService.update(policy);
                redirectAttributes.addFlashAttribute("success", "Policy updated successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/policies";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        policyService.deactivate(id);
        redirectAttributes.addFlashAttribute("success", "Policy deactivated");
        return "redirect:/policies";
    }
}
