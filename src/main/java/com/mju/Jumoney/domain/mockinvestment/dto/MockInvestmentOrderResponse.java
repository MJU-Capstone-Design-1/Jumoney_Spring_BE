package com.mju.Jumoney.domain.mockinvestment.dto;

import com.mju.Jumoney.domain.mockinvestment.enums.OrderType;

import java.math.BigDecimal;

public record MockInvestmentOrderResponse(
        Long orderId,
        OrderType orderType,
        Long stockId,
        String stockCode,
        String stockName,
        int quantity,
        BigDecimal executionPrice,
        BigDecimal totalExecutionAmount,
        BigDecimal cashBalance,
        BigDecimal totalPurchaseAmount
) {
}
