package com.mju.Jumoney.domain.mockinvestment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MockInvestmentChartCandleSyncResponse(
        String requestedStockCode,
        LocalDate targetDate,
        LocalDateTime requestedAt,
        List<String> requestedPeriods,
        List<SourceSync> sourceSyncs
) {

    public record SourceSync(
            String source,
            String intervalType,
            LocalDate fromDate,
            LocalDate toDate,
            int targetStockCount,
            int kisRequestCount,
            int successCount,
            int failureCount,
            int savedCandleCount,
            int skippedRecentCandleCount,
            List<Failure> failures
    ) {
    }

    public record Failure(
            String stockCode,
            String stockName,
            String reason
    ) {
    }
}
