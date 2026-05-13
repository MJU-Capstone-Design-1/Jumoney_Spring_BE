package com.mju.Jumoney.domain.stock.dto;

import java.math.BigDecimal;

public record StockCurrentPriceSnapshot(
        BigDecimal currentPrice,
        BigDecimal changeRate
) {
}
