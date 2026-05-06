package com.mju.Jumoney.global.client.kis.dto.dividend;

import java.math.BigDecimal;

// 배당금 계산에 필요한 배당일정 값을 숫자 타입으로 정규화한 DTO입니다.
public record KisDividendMetrics(
        String recordDate,
        String stockCode,
        String stockName,
        String dividendKind,
        BigDecimal cashDividendPerShare
) {
}
