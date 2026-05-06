package com.mju.Jumoney.global.client.kis.core;

import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class KisMetricCalculatorTest {

    private final KisMetricCalculator calculator = new KisMetricCalculator();

    @Test
    void calculatePayoutRatioReturnsPercentage() {
        BigDecimal payoutRatio = calculator.calculatePayoutRatio(
                BigDecimal.valueOf(1_488),
                BigDecimal.valueOf(6_564)
        );

        assertThat(payoutRatio).isEqualByComparingTo("22.6691");
    }

    @Test
    void calculatePayoutRatioReturnsNullWhenEpsIsNotPositive() {
        assertThat(calculator.calculatePayoutRatio(BigDecimal.valueOf(1_488), BigDecimal.ZERO))
                .isNull();
    }

    @Test
    void calculatePayoutRatioReturnsNullWhenDividendDataIsMissing() {
        assertThat(calculator.calculatePayoutRatio(
                null,
                new KisFinancialRatioMetrics(
                        "202512",
                        null,
                        null,
                        BigDecimal.valueOf(6_564),
                        null
                )
        )).isNull();
    }
}
