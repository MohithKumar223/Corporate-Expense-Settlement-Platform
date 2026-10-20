package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.enums.Role;
import com.company.expenseplatform.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           PasswordEncoder passwordEncoder,
                           AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    public Optional<Employee> findByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }

    public Optional<Employee> findByEmployeeCode(String code) {
        return employeeRepository.findByEmployeeCode(code);
    }

    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    public List<Employee> findActive() {
        return employeeRepository.findByActiveTrue();
    }

    public List<Employee> findByRole(Role role) {
        return employeeRepository.findByRole(role);
    }

    public List<Employee> findSubordinates(Long managerId) {
        return employeeRepository.findByManagerId(managerId);
    }

    public List<String> findDistinctDepartments() {
        return employeeRepository.findDistinctDepartments();
    }

    public boolean existsByEmail(String email) {
        return employeeRepository.existsByEmail(email);
    }

    public boolean existsByEmployeeCode(String code) {
        return employeeRepository.existsByEmployeeCode(code);
    }

    @Transactional
    public Employee register(Employee employee) {
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        if (employee.getRole() == null) {
            employee.setRole(Role.EMPLOYEE);
        }
        Employee saved = employeeRepository.save(employee);
        auditService.log("EMPLOYEE_CREATED", "Employee", saved.getId(),
                "Employee registered: " + saved.getEmployeeCode());
        return saved;
    }

    @Transactional
    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee update(Employee employee) {
        Employee saved = employeeRepository.save(employee);
        auditService.log("EMPLOYEE_UPDATED", "Employee", saved.getId(),
                "Employee updated: " + saved.getEmployeeCode());
        return saved;
    }

    @Transactional
    public void deactivate(Long id) {
        employeeRepository.findById(id).ifPresent(e -> {
            e.setActive(false);
            employeeRepository.save(e);
            auditService.log("EMPLOYEE_DEACTIVATED", "Employee", e.getId(),
                    "Employee deactivated: " + e.getEmployeeCode());
        });
    }

    @Transactional
    public void activate(Long id) {
        employeeRepository.findById(id).ifPresent(e -> {
            e.setActive(true);
            employeeRepository.save(e);
            auditService.log("EMPLOYEE_ACTIVATED", "Employee", e.getId(),
                    "Employee activated: " + e.getEmployeeCode());
        });
    }

    public long countActive() {
        return employeeRepository.countByActiveTrue();
    }

    public long countByRole(Role role) {
        return employeeRepository.countByRole(role);
    }

    public List<Employee> findManagers() {
        return employeeRepository.findByRole(Role.MANAGER);
    }
}
