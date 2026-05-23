package com.mju.Jumoney.domain.mockinvestment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MockInvestmentMinuteChartResponse(
        @Schema(description = "종목 코드", example = "005930")
        String stockCode,
        @Schema(description = "종목명", example = "삼성전자")
        String stockName,
        @Schema(description = "차트 봉 타입", example = "MINUTE")
        String intervalType,
        @Schema(description = "조회 날짜", example = "2026-05-21")
        LocalDate date,
        @Schema(description = "Redis 미확정 분봉 포함 여부. 오늘 날짜 조회에서 Redis 병합이 발생하면 true")
        boolean includesRealtime,
        @Schema(description = "응답에 포함된 마지막 확정 분봉 시각", example = "2026-05-21T14:00:00")
        LocalDateTime lastFinalCandleTime,
        @Schema(description = "분봉 캔들 목록")
        List<Candle> candles
) {

    public record Candle(
            @Schema(description = "캔들 기준 시각. KST 기준 1분봉 시작 시각", example = "2026-05-21T14:00:00")
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
            @Schema(description = "거래대금. KIS 응답 기준 누적 거래대금 필드이며, 분 단위 거래대금이 아닐 수 있음", example = "2275000000")
            Long tradeAmount,
            @Schema(description = "확정 캔들 여부. DB 확정 분봉은 true", example = "true")
            boolean isFinal
    ) {
    }
}
