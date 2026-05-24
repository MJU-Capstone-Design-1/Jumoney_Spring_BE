package com.mju.Jumoney.global.client.kis.dto.chart;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KisMinuteCandleMetrics(
        LocalDateTime candleTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume,
        Long tradeAmount
) {
}
