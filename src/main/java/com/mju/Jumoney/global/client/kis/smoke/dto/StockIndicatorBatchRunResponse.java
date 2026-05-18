package com.mju.Jumoney.global.client.kis.smoke.dto;

import com.mju.Jumoney.domain.stock.service.StockIndicatorBatchService;

import java.time.LocalDate;

public record StockIndicatorBatchRunResponse(
        LocalDate baseDate,
        String baseTime,
        int totalCount,
        int successCount,
        int failureCount
) {
    public static StockIndicatorBatchRunResponse from(StockIndicatorBatchService.StockIndicatorBatchResult result) {
        return new StockIndicatorBatchRunResponse(
                result.baseDate(),
                result.baseTime(),
                result.totalCount(),
                result.successCount(),
                result.failureCount()
        );
    }
}
