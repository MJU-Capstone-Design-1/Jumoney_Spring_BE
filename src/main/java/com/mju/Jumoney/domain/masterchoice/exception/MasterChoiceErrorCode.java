package com.mju.Jumoney.domain.masterchoice.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MasterChoiceErrorCode implements BaseErrorCode {
    STOCK_INDICATOR_BASE_TIME_NOT_FOUND(HttpStatus.BAD_REQUEST, "MASTER_CHOICE400_STOCK_INDICATOR_BASE_TIME", "추천에 사용할 종목 지표 데이터가 없습니다."),
    INVALID_MASTER_OPTION_SELECTION(HttpStatus.BAD_REQUEST, "MASTER_CHOICE400_OPTION", "선택한 거장에 사용할 수 없는 조건이 포함되어 있습니다."),
    MISSING_MASTER_SECTOR_SELECTION(HttpStatus.BAD_REQUEST, "MASTER_CHOICE400_SECTOR_REQUIRED", "선택한 거장 조건에는 섹터 선택이 필요합니다."),
    UNSUPPORTED_MASTER_SECTOR_SELECTION(HttpStatus.BAD_REQUEST, "MASTER_CHOICE400_SECTOR_UNSUPPORTED", "선택한 거장 조건에서는 섹터를 사용할 수 없습니다."),
    BACKTEST_CANDLE_DATA_NOT_FOUND(HttpStatus.BAD_REQUEST, "MASTER_CHOICE400_BACKTEST_CANDLE", "백테스팅에 사용할 일봉 데이터가 없습니다."),
    BACKTEST_FINANCIAL_DATA_NOT_FOUND(HttpStatus.BAD_REQUEST, "MASTER_CHOICE400_BACKTEST_FINANCIAL", "백테스팅에 사용할 재무 데이터가 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
