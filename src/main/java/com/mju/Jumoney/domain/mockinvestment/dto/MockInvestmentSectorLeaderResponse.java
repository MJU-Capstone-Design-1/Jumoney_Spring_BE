package com.mju.Jumoney.domain.mockinvestment.dto;

import java.math.BigDecimal;

public record MockInvestmentSectorLeaderResponse(
        Long sectorId,
        String sectorName,
        String stockCode,
        String stockName,
        BigDecimal currentPrice,
        BigDecimal changeRate
) {
}
