package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.ExpensePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpensePolicyRepository extends JpaRepository<ExpensePolicy, Long> {

    List<ExpensePolicy> findByActiveTrue();

    Optional<ExpensePolicy> findByCategoryNameAndActiveTrue(String categoryName);

    List<ExpensePolicy> findByCategoryNameContainingIgnoreCase(String categoryName);

    boolean existsByCategoryNameAndActiveTrue(String categoryName);
}
