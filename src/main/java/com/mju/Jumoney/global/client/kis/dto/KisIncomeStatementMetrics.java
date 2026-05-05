package com.mju.Jumoney.global.client.kis.dto;

import java.math.BigDecimal;

// 손익계산서 원문 문자열을 배치 계산에 쓰기 좋은 숫자 타입으로 정규화한 DTO입니다.
public record KisIncomeStatementMetrics(
        String settlementYearMonth,
        BigDecimal sales,
        BigDecimal operatingProfit
) {
}
