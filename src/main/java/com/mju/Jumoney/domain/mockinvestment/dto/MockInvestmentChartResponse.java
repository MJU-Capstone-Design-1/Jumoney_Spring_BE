package com.mju.Jumoney.domain.mockinvestment.dto;

import com.mju.Jumoney.domain.mockinvestment.enums.MockInvestmentChartPeriod;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MockInvestmentChartResponse(
        @Schema(description = "종목 코드", example = "005930")
        String stockCode,
        @Schema(description = "종목명", example = "삼성전자")
        String stockName,
        @Schema(description = "차트 기간", example = "ONE_DAY")
        MockInvestmentChartPeriod period,
        @Schema(description = "차트 봉 타입", example = "MINUTE")
        StockCandleIntervalType intervalType,
        @Schema(description = "조회 기준일. 생략 시 직전 개장일 기준으로 보정됩니다.", example = "2026-05-21")
        LocalDate date,
        @Schema(description = "Redis 미확정 캔들 포함 여부. 현재 구현은 ONE_DAY 또는 ONE_WEEK에서 true가 될 수 있습니다.")
        boolean includesRealtime,
        @Schema(description = "응답에 포함된 마지막 확정 봉 시각", example = "2026-05-21T14:00:00")
        LocalDateTime lastFinalCandleTime,
        @Schema(description = "차트 캔들 목록")
        List<Candle> candles
) {

    public record Candle(
            @Schema(description = "캔들 기준 시각", example = "2026-05-21T14:00:00")
            LocalDateTime candleTime,
            @Schema(description = "시가", example = "71000")
            BigDecimal openPrice,
            @Schema(description = "고가", example = "71200")
            BigDecimal highPrice,
            @Schema(description = "저가", example = "70900")
            BigDecimal lowPrice,
            @Schema(description = "종가", example = "71100")
            BigDecimal closePrice,
            @Schema(description = "거래량", example = "32000")
            Long volume,
            @Schema(description = "거래대금", example = "2275000000")
            Long tradeAmount,
            @Schema(description = "확정 캔들 여부", example = "true")
            boolean isFinal
    ) {
    }
}
