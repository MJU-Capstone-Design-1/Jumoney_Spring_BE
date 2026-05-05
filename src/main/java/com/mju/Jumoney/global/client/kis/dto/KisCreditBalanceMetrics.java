package com.mju.Jumoney.global.client.kis.dto;

import java.math.BigDecimal;

// 신용잔고 추천 조건에 필요한 값을 숫자 타입으로 정규화한 DTO입니다.
public record KisCreditBalanceMetrics(
        String tradeDate,
        String settlementDate,
        BigDecimal totalLoanBalanceRate
) {
}
