package com.mju.Jumoney.domain.stock.dto;

public record MinuteCandleSyncFailureResponse(
        String stockCode,
        String stockName,
        String message
) {
}
