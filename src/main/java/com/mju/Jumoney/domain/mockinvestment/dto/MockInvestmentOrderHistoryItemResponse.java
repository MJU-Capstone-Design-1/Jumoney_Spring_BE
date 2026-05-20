package com.mju.Jumoney.domain.mockinvestment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MockInvestmentOrderHistoryItemResponse(
        Long orderId,
        String orderType,
        String stockCode,
        String stockName,
        BigDecimal executionPrice,
        Integer quantity,
        BigDecimal totalExecutionAmount,
        LocalDateTime executedAt
) {
}
