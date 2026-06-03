package com.mju.Jumoney.global.client.kis.dto.finance;

import java.math.BigDecimal;

// 서비스/배치 계층에서 바로 사용할 수 있도록 KIS 재무비율 문자열을 숫자로 정규화한 DTO입니다.
public record KisFinancialRatioMetrics(
        String settlementYearMonth,
        BigDecimal salesGrowthRate,
        BigDecimal operatingProfitGrowthRate,
        BigDecimal roe,
        BigDecimal eps,
        BigDecimal debtRatio
) {
}
