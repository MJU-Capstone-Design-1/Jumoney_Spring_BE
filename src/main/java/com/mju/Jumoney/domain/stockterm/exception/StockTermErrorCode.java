package com.mju.Jumoney.domain.stockterm.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StockTermErrorCode implements BaseErrorCode {
    STOCK_TERM_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK_TERM404_CATEGORY", "주식 용어 카테고리를 찾을 수 없습니다."),
    STOCK_TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK_TERM404", "주식 용어를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
