package com.mju.Jumoney.domain.home.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HomeMockInvestmentChartResponse(
        boolean hasAccount,
        Long stockId,
        String stockCode,
        String stockName,
        LocalDate date,
        Boolean includesRealtime,
        List<Candle> candles
) {

    public record Candle(
            LocalDateTime candleTime,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice
    ) {
    }
}
