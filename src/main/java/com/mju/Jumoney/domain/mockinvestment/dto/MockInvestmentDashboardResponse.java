package com.mju.Jumoney.domain.mockinvestment.dto;

import java.math.BigDecimal;

public record MockInvestmentDashboardResponse(
        Long accountId,
        BigDecimal seedMoney,
        BigDecimal cashBalance,
        BigDecimal totalPurchaseAmount,
        BigDecimal totalEvaluationAmount,
        BigDecimal totalAsset,
        BigDecimal totalProfitAmount,
        BigDecimal totalProfitRate
) {
}
