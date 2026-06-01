package com.mju.Jumoney.domain.masterchoice.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MasterChoiceBacktestResponse(
        @Schema(description = "거장 ID", example = "1")
        Long masterId,
        @Schema(description = "거장 코드", example = "WARREN_BUFFETT")
        MasterCode masterCode,
        @Schema(description = "거장 이름", example = "워런 버핏")
        String masterName,
        @Schema(description = "종목 ID", example = "1")
        Long stockId,
        @Schema(description = "종목 코드", example = "005930")
        String stockCode,
        @Schema(description = "종목명", example = "삼성전자")
        String stockName,
        @Schema(description = "조회 시작일", example = "2025-06-01")
        LocalDate fromDate,
        @Schema(description = "조회 종료일", example = "2026-06-01")
        LocalDate toDate,
        @Schema(description = "적용한 조건 목록")
        List<MasterOptionLogicCode> selectedLogicCodes,
        @Schema(description = "차트 일봉 목록")
        List<Candle> candles,
        @Schema(description = "조건 만족 구간 목록")
        List<MatchedRange> matchedRanges,
        @Schema(description = "거래일별 조건 평가 결과")
        List<DailyEvaluation> dailyEvaluations
) {

    public record Candle(
            LocalDate date,
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice,
            BigDecimal closePrice,
            Long volume,
            Long tradeAmount
    ) {
    }

    public record MatchedRange(
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    public record DailyEvaluation(
            LocalDate date,
            boolean matched,
            List<MasterOptionLogicCode> matchedLogicCodes,
            int matchedConditionCount,
            int totalConditionCount,
            String financialBaseYearMonth
    ) {
    }
}
