package com.mju.Jumoney.domain.mockinvestment.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MockInvestmentErrorCode implements BaseErrorCode {
    MARKET_LEADER_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "MOCK_INVESTMENT404_MARKET_LEADER", "해당 섹터의 대장주를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
