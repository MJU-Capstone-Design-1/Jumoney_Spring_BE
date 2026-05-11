package com.mju.Jumoney.domain.stock.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StockErrorCode implements BaseErrorCode {
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK404", "종목을 찾을 수 없습니다."),
    INVALID_MARKET_TYPE(HttpStatus.BAD_REQUEST, "STOCK400_MARKET", "유효하지 않은 시장 구분 코드입니다."),
    STOCK_INDICATOR_CURRENT_FINANCIAL_RATIO_MISSING(HttpStatus.BAD_REQUEST, "STOCK_INDICATOR400_CURRENT_FINANCIAL_RATIO", "현재 재무비율 데이터가 비어있습니다."),
    STOCK_INDICATOR_CURRENT_INCOME_STATEMENT_MISSING(HttpStatus.BAD_REQUEST, "STOCK_INDICATOR400_CURRENT_INCOME_STATEMENT", "현재 손익계산서 데이터가 비어있습니다."),
    STOCK_INDICATOR_REQUIRED_METRIC_MISSING(HttpStatus.BAD_REQUEST, "STOCK_INDICATOR400_REQUIRED_METRIC", "종목 지표 필수 값이 비어있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
