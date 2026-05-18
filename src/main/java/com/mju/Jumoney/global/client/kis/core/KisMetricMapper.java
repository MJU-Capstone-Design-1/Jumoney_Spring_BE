package com.mju.Jumoney.global.client.kis.core;

import com.mju.Jumoney.global.client.kis.dto.dividend.KisDividendMetrics;
import com.mju.Jumoney.global.client.kis.dto.dividend.KisDividendOutput;
import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioOutput;
import com.mju.Jumoney.global.client.kis.dto.finance.KisIncomeStatementMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisIncomeStatementOutput;
import com.mju.Jumoney.global.client.kis.dto.price.KisCurrentPriceMetrics;
import com.mju.Jumoney.global.client.kis.dto.price.KisCurrentPriceOutput;
import com.mju.Jumoney.global.client.kis.dto.price.KisExecutionStrengthMetrics;
import com.mju.Jumoney.global.client.kis.dto.price.KisExecutionStrengthOutput;
import com.mju.Jumoney.global.client.kis.dto.trading.KisCreditBalanceMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisCreditBalanceOutput;
import com.mju.Jumoney.global.client.kis.dto.trading.KisInvestorTradeDailyMetrics;
import com.mju.Jumoney.global.client.kis.dto.trading.KisInvestorTradeDailyOutput;
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
                toBigDecimal(output.changeRate()),
                toBigDecimal(output.marketCap()),
                toBigDecimal(output.per()),
                toBigDecimal(output.pbr()),
                toBigDecimal(output.accumulatedTradeAmount()),
                toBigDecimal(output.twoHundredFiftyDayHighPriceRate()),
                toBigDecimal(output.fiftyTwoWeekHighPriceRate())
        );
    }

    public KisExecutionStrengthMetrics toExecutionStrengthMetrics(KisExecutionStrengthOutput output) {
        return new KisExecutionStrengthMetrics(
                output.executionTime(),
                toBigDecimal(output.currentPrice()),
                toBigDecimal(output.executionVolume()),
                toBigDecimal(output.executionStrength())
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

    public KisIncomeStatementMetrics toIncomeStatementMetrics(KisIncomeStatementOutput output) {
        return new KisIncomeStatementMetrics(
                output.settlementYearMonth(),
                toBigDecimal(output.sales()),
                toBigDecimal(output.operatingProfit())
        );
    }

    public KisDividendMetrics toDividendMetrics(KisDividendOutput output) {
        return new KisDividendMetrics(
                output.recordDate(),
                output.stockCode(),
                output.stockName(),
                output.dividendKind(),
                toBigDecimal(output.cashDividendPerShare())
        );
    }

    public KisCreditBalanceMetrics toCreditBalanceMetrics(KisCreditBalanceOutput output) {
        return new KisCreditBalanceMetrics(
                output.tradeDate(),
                output.settlementDate(),
                toBigDecimal(output.totalLoanBalanceRate())
        );
    }

    public KisInvestorTradeDailyMetrics toInvestorTradeDailyMetrics(KisInvestorTradeDailyOutput output) {
        return new KisInvestorTradeDailyMetrics(
                output.businessDate(),
                toBigDecimal(output.institutionNetBuyQuantity())
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
