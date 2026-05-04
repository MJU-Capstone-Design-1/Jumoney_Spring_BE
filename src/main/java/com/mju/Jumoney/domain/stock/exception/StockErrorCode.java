package com.mju.Jumoney.domain.stock.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StockErrorCode implements BaseErrorCode {
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK404", "종목을 찾을 수 없습니다."),
    INVALID_MARKET_TYPE(HttpStatus.BAD_REQUEST, "STOCK400_MARKET", "유효하지 않은 시장 구분 코드입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
