package com.mju.Jumoney.domain.mockinvestment.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MockInvestmentErrorCode implements BaseErrorCode {
    MOCK_INVESTMENT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "MOCK_INVESTMENT404_ACCOUNT", "모의투자 계좌를 찾을 수 없습니다."),
    MARKET_LEADER_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "MOCK_INVESTMENT404_MARKET_LEADER", "해당 섹터의 대장주를 찾을 수 없습니다."),
    INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "MOCK_INVESTMENT400_QUANTITY", "주문 수량은 1주 이상이어야 합니다."),
    CURRENT_PRICE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "MOCK_INVESTMENT400_PRICE", "현재가를 조회할 수 없습니다."),
    INSUFFICIENT_CASH_BALANCE(HttpStatus.BAD_REQUEST, "MOCK_INVESTMENT400_CASH", "예수금이 부족합니다."),
    INSUFFICIENT_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "MOCK_INVESTMENT400_STOCK_QUANTITY", "보유 수량이 부족합니다."),
    MARKET_CLOSED(HttpStatus.BAD_REQUEST, "MOCK_INVESTMENT400_MARKET_CLOSED", "장 중(9:00 ~ 15:20)에만 주문할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
