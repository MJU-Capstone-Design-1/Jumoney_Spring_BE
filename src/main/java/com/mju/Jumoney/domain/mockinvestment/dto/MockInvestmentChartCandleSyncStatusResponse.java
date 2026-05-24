package com.mju.Jumoney.domain.mockinvestment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MockInvestmentChartCandleSyncStatusResponse(
        String stockCode,
        String stockName,
        LocalDate targetDate,
        List<PeriodStatus> periods
) {

    public record PeriodStatus(
            String period,
            String intervalType,
            LocalDateTime expectedStartTime,
            LocalDateTime expectedEndTime,
            Long expectedCandleCount,
            long candleCount,
            LocalDateTime firstCandleTime,
            LocalDateTime lastCandleTime,
            boolean hasAnyCandle,
            boolean hasExpectedCandleCount,
            boolean coversExpectedRange,
            boolean complete,
            String message
    ) {
    }
}
