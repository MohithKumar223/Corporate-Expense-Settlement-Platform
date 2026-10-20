package com.company.expenseplatform.config;

import com.company.expenseplatform.entity.AdvanceRequest;
import com.company.expenseplatform.entity.Employee;
import com.company.expenseplatform.entity.Expense;
import com.company.expenseplatform.entity.ExpensePolicy;
import com.company.expenseplatform.entity.enums.AdvanceRequestStatus;
import com.company.expenseplatform.entity.enums.ExpenseStatus;
import com.company.expenseplatform.entity.enums.Role;
import com.company.expenseplatform.repository.AdvanceRequestRepository;
import com.company.expenseplatform.repository.EmployeeRepository;
import com.company.expenseplatform.repository.ExpensePolicyRepository;
import com.company.expenseplatform.repository.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EmployeeRepository employeeRepository;
    private final ExpensePolicyRepository policyRepository;
    private final AdvanceRequestRepository advanceRepository;
    private final ExpenseRepository expenseRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(EmployeeRepository employeeRepository,
                           ExpensePolicyRepository policyRepository,
                           AdvanceRequestRepository advanceRepository,
                           ExpenseRepository expenseRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.policyRepository = policyRepository;
        this.advanceRepository = advanceRepository;
        this.expenseRepository = expenseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.count() == 0) {
            log.info("Bootstrapping initial database users...");

            // 1. Finance Admin
            Employee admin = new Employee();
            admin.setFullName("Finance Administrator");
            admin.setEmail("finance@company.com");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setEmployeeCode("EMP001");
            admin.setDepartment("Finance");
            admin.setDesignation("Finance Controller");
            admin.setRole(Role.FINANCE_ADMIN);
            admin.setActive(true);
            employeeRepository.save(admin);

            // 2. Manager
            Employee manager = new Employee();
            manager.setFullName("Operations Manager");
            manager.setEmail("manager@company.com");
            manager.setPassword(passwordEncoder.encode("password123"));
            manager.setEmployeeCode("EMP002");
            manager.setDepartment("Engineering");
            manager.setDesignation("Engineering Lead");
            manager.setRole(Role.MANAGER);
            manager.setActive(true);
            Employee savedManager = employeeRepository.save(manager);

            // 3. Employee
            Employee employee = new Employee();
            employee.setFullName("Alex Turner");
            employee.setEmail("employee@company.com");
            employee.setPassword(passwordEncoder.encode("password123"));
            employee.setEmployeeCode("EMP003");
            employee.setDepartment("Engineering");
            employee.setDesignation("Senior Software Engineer");
            employee.setRole(Role.EMPLOYEE);
            employee.setManager(savedManager);
            employee.setActive(true);
            Employee savedEmployee = employeeRepository.save(employee);

            log.info("Default users seeded: finance@company.com, manager@company.com, employee@company.com");

            // 4. Default Policies
            if (policyRepository.count() == 0) {
                createPolicy("Travel", new BigDecimal("50000.00"), true, "Domestic and international corporate travel");
                createPolicy("Accommodation", new BigDecimal("35000.00"), true, "Hotel lodging and guest stays");
                createPolicy("Meals", new BigDecimal("10000.00"), true, "Daily subsistence and client dining");
                createPolicy("Equipment", new BigDecimal("25000.00"), true, "Computer peripherals and project hardware");
                createPolicy("Office Supplies", new BigDecimal("5000.00"), false, "Stationery and miscellaneous supplies");
                log.info("Default policies seeded.");
            }

            // 5. Initial Sample Advance & Expense
            AdvanceRequest advance = new AdvanceRequest();
            advance.setEmployee(savedEmployee);
            advance.setDepartment(savedEmployee.getDepartment());
            advance.setPurpose("Client Site Deployment in Mumbai");
            advance.setCategory("Travel");
            advance.setRequestedAmount(new BigDecimal("15000.00"));
            advance.setStartDate(LocalDate.now().plusDays(3));
            advance.setEndDate(LocalDate.now().plusDays(8));
            advance.setSupportingInfo("Express train ₹3,500, Hotel 3 nights ₹7,500, Food allowance ₹4,000");
            advance.setStatus(AdvanceRequestStatus.APPROVED);
            AdvanceRequest savedAdvance = advanceRepository.save(advance);

            Expense expense = new Expense();
            expense.setEmployee(savedEmployee);
            expense.setAdvanceRequest(savedAdvance);
            expense.setCategory("Travel");
            expense.setAmount(new BigDecimal("3500.00"));
            expense.setExpenseDate(LocalDate.now());
            expense.setDescription("Roundtrip Express Train ticket to Mumbai site");
            expense.setStatus(ExpenseStatus.APPROVED);
            expenseRepository.save(expense);

            log.info("Sample live workflow demonstration data seeded.");
        }
    }

    private void createPolicy(String category, BigDecimal maxAmount, boolean receiptRequired, String desc) {
        ExpensePolicy policy = new ExpensePolicy();
        policy.setCategoryName(category);
        policy.setMaxAmount(maxAmount);
        policy.setRequiresReceipt(receiptRequired);
        policy.setDescription(desc);
        policy.setActive(true);
        policyRepository.save(policy);
    }
}
