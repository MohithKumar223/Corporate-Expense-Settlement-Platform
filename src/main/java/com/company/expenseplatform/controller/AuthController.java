package com.company.expenseplatform.controller;

import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.enums.Role;
import com.company.expenseplatform.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final EmployeeService employeeService;

    public AuthController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("employee", new Employee());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute Employee employee,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        if (employeeService.existsByEmail(employee.getEmail())) {
            result.rejectValue("email", "error.employee", "Email already exists");
        }
        if (employeeService.existsByEmployeeCode(employee.getEmployeeCode())) {
            result.rejectValue("employeeCode", "error.employee", "Employee code already exists");
        }
        if (employee.getPassword() == null || employee.getPassword().length() < 6) {
            result.rejectValue("password", "error.employee", "Password must be at least 6 characters");
        }
        if (result.hasErrors()) {
            return "register";
        }

        employee.setRole(Role.EMPLOYEE);
        employee.setActive(true);
        employeeService.register(employee);
        redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
        return "redirect:/login";
    }
}
