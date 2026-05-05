package com.mju.Jumoney.global.client.kis.dto;

import java.math.BigDecimal;

// 서비스/배치 계층에서 바로 사용할 수 있도록 KIS 현재가 시세 문자열을 숫자로 정규화한 DTO입니다.
public record KisCurrentPriceMetrics(
        BigDecimal currentPrice,
        BigDecimal marketCap,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal accumulatedTradeAmount,
        BigDecimal twoHundredFiftyDayHighPriceRate,
        BigDecimal fiftyTwoWeekHighPriceRate
) {
}
