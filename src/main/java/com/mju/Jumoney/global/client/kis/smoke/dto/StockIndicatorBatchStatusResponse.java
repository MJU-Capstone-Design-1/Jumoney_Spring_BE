package com.mju.Jumoney.global.client.kis.smoke.dto;

import java.time.LocalDate;
import java.util.List;

public record StockIndicatorBatchStatusResponse(
        LocalDate baseDate,
        String baseTime,
        long stockCount,
        long indicatorCount,
        long missingCount,
        long invalidRequiredFieldCount,
        boolean complete,
        List<MissingStockIndicatorResponse> missingStocks
) {
}
