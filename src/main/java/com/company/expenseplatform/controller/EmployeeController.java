package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.enums.Role;
import com.company.expenseplatform.service.EmployeeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String list(Model model) {
        model.addAttribute("employees", employeeService.findAll());
        return "employees/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("employee", new Employee());
        model.addAttribute("roles", Role.values());
        model.addAttribute("managers", employeeService.findManagers());
        return "employees/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String save(@ModelAttribute Employee employee,
                       @RequestParam(required = false) Long managerId,
                       RedirectAttributes redirectAttributes) {
        try {
            if (employee.getId() == null) {
                // New employee
                if (employeeService.existsByEmail(employee.getEmail())) {
                    redirectAttributes.addFlashAttribute("error", "Email already exists");
                    return "redirect:/employees/create";
                }
                if (employeeService.existsByEmployeeCode(employee.getEmployeeCode())) {
                    redirectAttributes.addFlashAttribute("error", "Employee code already exists");
                    return "redirect:/employees/create";
                }
                if (managerId != null) {
                    employee.setManager(employeeService.findById(managerId).orElse(null));
                }
                employeeService.register(employee);
                redirectAttributes.addFlashAttribute("success", "Employee created successfully");
            } else {
                // Update existing
                Employee existing = employeeService.findById(employee.getId())
                        .orElseThrow(() -> new RuntimeException("Employee not found"));
                existing.setFullName(employee.getFullName());
                existing.setEmail(employee.getEmail());
                existing.setDepartment(employee.getDepartment());
                existing.setDesignation(employee.getDesignation());
                existing.setRole(employee.getRole());
                if (managerId != null) {
                    existing.setManager(employeeService.findById(managerId).orElse(null));
                } else {
                    existing.setManager(null);
                }
                employeeService.update(existing);
                redirectAttributes.addFlashAttribute("success", "Employee updated successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/employees";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        model.addAttribute("employee", employee);
        model.addAttribute("roles", Role.values());
        model.addAttribute("managers", employeeService.findManagers());
        return "employees/form";
    }

    @GetMapping("/view/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String view(@PathVariable Long id, Model model) {
        Employee employee = employeeService.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        model.addAttribute("employee", employee);
        return "employees/view";
    }

    @PostMapping("/deactivate/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String deactivate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.deactivate(id);
        redirectAttributes.addFlashAttribute("success", "Employee deactivated");
        return "redirect:/employees";
    }

    @PostMapping("/activate/{id}")
    @PreAuthorize("hasRole('FINANCE_ADMIN')")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        employeeService.activate(id);
        redirectAttributes.addFlashAttribute("success", "Employee activated");
        return "redirect:/employees";
    }
}
