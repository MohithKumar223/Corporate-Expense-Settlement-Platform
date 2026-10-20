package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.ExceptionRecord;
import com.company.expenseplatform.service.ExceptionService;
import com.company.expenseplatform.service.EmployeeService;
import com.company.expenseplatform.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/exceptions")
@PreAuthorize("hasRole('FINANCE_ADMIN')")
public class ExceptionController {

    private final ExceptionService exceptionService;
    private final EmployeeService employeeService;

    public ExceptionController(ExceptionService exceptionService, EmployeeService employeeService) {
        this.exceptionService = exceptionService;
        this.employeeService = employeeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("exceptions", exceptionService.findAll());
        return "exceptions/list";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        ExceptionRecord ex = exceptionService.findById(id)
                .orElseThrow(() -> new RuntimeException("Exception not found"));
        model.addAttribute("exception", ex);
        return "exceptions/view";
    }

    @PostMapping("/investigate/{id}")
    public String investigate(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails user,
                              RedirectAttributes redirectAttributes) {
        try {
            Employee investigator = employeeService.findById(user.getId()).orElse(null);
            exceptionService.investigate(id, investigator);
            redirectAttributes.addFlashAttribute("success", "Investigation started");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/exceptions/view/" + id;
    }

    @PostMapping("/resolve/{id}")
    public String resolve(@PathVariable Long id,
                          @RequestParam String resolution,
                          RedirectAttributes redirectAttributes) {
        try {
            exceptionService.resolve(id, resolution);
            redirectAttributes.addFlashAttribute("success", "Exception resolved");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/exceptions";
    }

    @PostMapping("/close/{id}")
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        exceptionService.close(id);
        redirectAttributes.addFlashAttribute("success", "Exception closed");
        return "redirect:/exceptions";
    }
}
