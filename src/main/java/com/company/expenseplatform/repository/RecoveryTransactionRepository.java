package com.company.expenseplatform.repository;

import com.company.expenseplatform.entity.RecoveryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RecoveryTransactionRepository extends JpaRepository<RecoveryTransaction, Long> {

    List<RecoveryTransaction> findBySettlementId(Long settlementId);

    @Query("SELECT COALESCE(SUM(r.amountRecovered), 0) FROM RecoveryTransaction r WHERE r.settlement.id = :sid")
    BigDecimal sumRecoveredBySettlement(@Param("sid") Long settlementId);

    @Query("SELECT COALESCE(SUM(r.amountRecovered), 0) FROM RecoveryTransaction r")
    BigDecimal sumTotalRecovered();
}
