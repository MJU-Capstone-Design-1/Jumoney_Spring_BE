package com.mju.Jumoney.domain.verifiedoperation.dto;

import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderHistoryItemResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentPortfolioItemResponse;
import com.mju.Jumoney.domain.verifiedoperation.enums.VerifiedOperationAccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VerifiedOperationAccountDetailResponse(
        String operationDescription,
        String accountCode,
        String accountName,
        VerifiedOperationAccountType type,
        List<VerifiedOperationAccountSummaryResponse.ConditionResponse> usedConditions,
        BigDecimal totalPurchaseAmount,
        BigDecimal totalEvaluationAmount,
        BigDecimal totalProfitAmount,
        BigDecimal totalProfitRate,
        Integer holdingStockCount,
        LocalDateTime lastTradedAt,
        List<MockInvestmentPortfolioItemResponse> holdings,
        List<MockInvestmentOrderHistoryItemResponse> recentOrders
) {
}
