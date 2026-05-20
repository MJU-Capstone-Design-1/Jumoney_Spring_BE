package com.mju.Jumoney.domain.mockinvestment.dto;

import java.math.BigDecimal;

public record MockInvestmentPortfolioItemResponse(
        Long stockId,
        String stockCode,
        String stockName,
        String sectorName,
        Integer quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitAmount,
        BigDecimal profitRate,
        BigDecimal changeRate
) {
}
