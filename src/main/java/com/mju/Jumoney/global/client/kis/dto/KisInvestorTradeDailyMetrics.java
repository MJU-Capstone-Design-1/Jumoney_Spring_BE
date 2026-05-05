package com.mju.Jumoney.global.client.kis.dto;

import java.math.BigDecimal;

// 기관 순매수 합산 계산에 필요한 일별 투자자매매동향 값입니다.
public record KisInvestorTradeDailyMetrics(
        String businessDate,
        BigDecimal institutionNetBuyQuantity
) {
}
