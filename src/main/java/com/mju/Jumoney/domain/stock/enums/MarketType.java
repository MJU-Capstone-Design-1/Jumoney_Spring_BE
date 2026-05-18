package com.mju.Jumoney.domain.stock.enums;

import com.mju.Jumoney.domain.stock.exception.StockErrorCode;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum MarketType {
    KOSPI("J"),
    KOSDAQ("Q");

    private final String code;

    public static MarketType fromCode(String code) {
        return Arrays.stream(values())
                .filter(m -> m.code.equals(code.trim()))
                .findFirst()
                .orElseThrow(() -> new CustomException(StockErrorCode.INVALID_MARKET_TYPE));
    }
}
