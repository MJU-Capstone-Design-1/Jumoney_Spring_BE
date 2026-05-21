package com.mju.Jumoney.domain.mockinvestment.dto;

import java.math.BigDecimal;
import java.util.List;

public record MockInvestmentSectorStockItemResponse(
        Long stockId,
        String stockCode,
        String stockName,
        BigDecimal currentPrice,
        BigDecimal changeRate,
        boolean isMarketLeader,
        List<String> tags
) {
}
