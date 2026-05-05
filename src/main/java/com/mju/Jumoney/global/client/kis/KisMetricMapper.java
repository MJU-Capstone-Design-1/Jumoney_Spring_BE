package com.mju.Jumoney.global.client.kis;

import com.mju.Jumoney.global.client.kis.dto.KisCurrentPriceMetrics;
import com.mju.Jumoney.global.client.kis.dto.KisCurrentPriceOutput;
import com.mju.Jumoney.global.client.kis.dto.KisFinancialRatioMetrics;
import com.mju.Jumoney.global.client.kis.dto.KisFinancialRatioOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

// KIS 응답 파싱
// 문자열 숫자를 애플리케이션에서 쓰기 좋은 BigDecimal로 변환합니다.
@Slf4j
@Component
public class KisMetricMapper {

    public KisCurrentPriceMetrics toCurrentPriceMetrics(KisCurrentPriceOutput output) {
        return new KisCurrentPriceMetrics(
                toBigDecimal(output.currentPrice()),
                toBigDecimal(output.marketCap()),
                toBigDecimal(output.per()),
                toBigDecimal(output.pbr()),
                toBigDecimal(output.accumulatedTradeAmount()),
                toBigDecimal(output.twoHundredFiftyDayHighPriceRate()),
                toBigDecimal(output.fiftyTwoWeekHighPriceRate())
        );
    }

    public KisFinancialRatioMetrics toFinancialRatioMetrics(KisFinancialRatioOutput output) {
        return new KisFinancialRatioMetrics(
                output.settlementYearMonth(),
                toBigDecimal(output.operatingProfitGrowthRate()),
                toBigDecimal(output.roe()),
                toBigDecimal(output.eps()),
                toBigDecimal(output.debtRatio())
        );
    }

    private BigDecimal toBigDecimal(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.replace(",", "").trim();
        if ("-".equals(normalized)) {
            return null;
        }

        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            log.warn("[KIS] 숫자 변환 실패 (null 처리됨): value='{}'", value);
            return null;
        }
    }
}
