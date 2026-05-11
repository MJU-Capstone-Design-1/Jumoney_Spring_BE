package com.mju.Jumoney.global.client.kis.dto.price;

import java.math.BigDecimal;

// 추천 초단기 정렬에 필요한 체결강도 값입니다.
public record KisExecutionStrengthMetrics(
        String executionTime,
        BigDecimal currentPrice,
        BigDecimal executionVolume,
        BigDecimal executionStrength
) {
}
