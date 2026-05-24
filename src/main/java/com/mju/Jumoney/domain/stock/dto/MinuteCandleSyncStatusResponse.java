package com.mju.Jumoney.domain.stock.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MinuteCandleSyncStatusResponse(
        String stockCode,
        String stockName,
        LocalDate date,
        LocalDateTime dbExpectedStartTime,
        LocalDateTime dbExpectedEndTime,
        long dbExpectedCandleCount,
        long candleCount,
        LocalDateTime firstCandleTime,
        LocalDateTime lastCandleTime,
        boolean hasAnyCandle,
        boolean hasExpectedCandleCount,
        boolean coversExpectedRange,
        boolean realtimeCheckRequired,
        LocalDateTime realtimeExpectedStartTime,
        LocalDateTime realtimeExpectedEndTime,
        boolean realtimeRedisChecked,
        String realtimeRedisCheckMessage
) {
}
