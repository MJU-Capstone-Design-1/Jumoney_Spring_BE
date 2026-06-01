package com.mju.Jumoney.domain.masterchoice.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record MasterChoiceBacktestResponse(
        @Schema(description = "거장 ID", example = "1")
        Long masterId,
        @Schema(description = "거장 코드", example = "WARREN_BUFFETT")
        MasterCode masterCode,
        @Schema(description = "종목 코드", example = "005930")
        String stockCode,
        @Schema(description = "조회 시작일", example = "2025-06-01")
        LocalDate fromDate,
        @Schema(description = "조회 종료일", example = "2026-06-01")
        LocalDate toDate,
        @Schema(description = "적용한 조건 목록")
        List<MasterOptionLogicCode> selectedLogicCodes,
        @Schema(description = "거래일별 백테스트 조건 만족 여부")
        List<DailyResult> dailyResults,
        @Schema(description = "백테스트 원천 데이터 부족 경고 목록")
        List<DataWarning> dataWarnings
) {

    public record DailyResult(
            LocalDate date,
            boolean matched
    ) {
    }

    public record DataWarning(
            LocalDate date,
            String code,
            String message
    ) {
    }
}
