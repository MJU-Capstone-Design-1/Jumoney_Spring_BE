package com.mju.Jumoney.domain.stock.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MinuteCandleSyncResponse(
        String requestedStockCode,
        LocalDateTime requestedAt,
        LocalDateTime finalizationCutoffTime,
        int finalizationBufferMinutes,
        int targetStockCount,
        int kisRequestCount,
        int successCount,
        int failureCount,
        int savedCandleCount,
        int skippedRecentCandleCount,
        List<MinuteCandleSyncFailureResponse> failures
) {
}
