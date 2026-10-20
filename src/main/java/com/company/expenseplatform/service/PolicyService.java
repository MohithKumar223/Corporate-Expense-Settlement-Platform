package com.company.expenseplatform.service;

import com.company.expenseplatform.entity.ExpensePolicy;
import com.company.expenseplatform.repository.ExpensePolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PolicyService {

    private final ExpensePolicyRepository policyRepository;
    private final AuditService auditService;

    public PolicyService(ExpensePolicyRepository policyRepository, AuditService auditService) {
        this.policyRepository = policyRepository;
        this.auditService = auditService;
    }

    public List<ExpensePolicy> findAll() {
        return policyRepository.findAll();
    }

    public List<ExpensePolicy> findActive() {
        return policyRepository.findByActiveTrue();
    }

    public Optional<ExpensePolicy> findById(Long id) {
        return policyRepository.findById(id);
    }

    public Optional<ExpensePolicy> findActiveByCategory(String categoryName) {
        return policyRepository.findByCategoryNameAndActiveTrue(categoryName);
    }

    @Transactional
    public ExpensePolicy save(ExpensePolicy policy) {
        ExpensePolicy saved = policyRepository.save(policy);
        auditService.log("POLICY_CREATED", "ExpensePolicy", saved.getId(),
                "Policy created: " + saved.getCategoryName());
        return saved;
    }

    @Transactional
    public ExpensePolicy update(ExpensePolicy policy) {
        ExpensePolicy saved = policyRepository.save(policy);
        auditService.log("POLICY_UPDATED", "ExpensePolicy", saved.getId(),
                "Policy updated: " + saved.getCategoryName());
        return saved;
    }

    @Transactional
    public void deactivate(Long id) {
        policyRepository.findById(id).ifPresent(p -> {
            p.setActive(false);
            policyRepository.save(p);
            auditService.log("POLICY_DEACTIVATED", "ExpensePolicy", p.getId(),
                    "Policy deactivated: " + p.getCategoryName());
        });
    }
}
