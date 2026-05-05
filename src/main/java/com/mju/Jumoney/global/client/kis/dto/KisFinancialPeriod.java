package com.mju.Jumoney.global.client.kis.dto;

// KIS 재무비율 API의 FID_DIV_CLS_CODE 값입니다.
public enum KisFinancialPeriod {
    YEAR("0"),
    QUARTER("1");

    private final String code;

    KisFinancialPeriod(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
