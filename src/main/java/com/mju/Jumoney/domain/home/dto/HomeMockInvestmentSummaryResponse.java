package com.mju.Jumoney.domain.home.dto;

import java.math.BigDecimal;

public record HomeMockInvestmentSummaryResponse(
        boolean hasAccount,
        BigDecimal totalPurchaseAmount,
        BigDecimal totalProfitAmount,
        BigDecimal totalProfitRate,
        TopHolding topHolding
) {

    public record TopHolding(
            Long stockId,
            String stockCode,
            String stockName,
            BigDecimal purchaseAmount,
            BigDecimal profitAmount,
            BigDecimal profitRate
    ) {
    }
}
