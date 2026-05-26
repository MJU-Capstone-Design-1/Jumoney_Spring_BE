package com.mju.Jumoney.domain.verifiedoperation.repository;

import com.mju.Jumoney.domain.verifiedoperation.domain.VerifiedOperationTradeLot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface VerifiedOperationTradeLotRepository extends JpaRepository<VerifiedOperationTradeLot, Long> {

    @EntityGraph(attributePaths = {"stock"})
    List<VerifiedOperationTradeLot> findByAccountCodeAndRemainingQuantityGreaterThanAndSellDueAtBeforeOrderByBoughtAtAsc(
            String accountCode,
            int remainingQuantity,
            LocalDateTime sellDueAt
    );
}
