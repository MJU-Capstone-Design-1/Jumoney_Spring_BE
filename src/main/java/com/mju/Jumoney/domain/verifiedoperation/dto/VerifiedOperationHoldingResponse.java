package com.mju.Jumoney.domain.verifiedoperation.dto;

import java.math.BigDecimal;

public record VerifiedOperationHoldingResponse(
        Long stockId,
        String stockCode,
        String stockName,
        String sectorName,
        Integer quantity,
        BigDecimal averagePurchasePrice,
        BigDecimal currentPrice,
        BigDecimal evaluationAmount,
        BigDecimal profitAmount,
        BigDecimal profitRate
) {
}
