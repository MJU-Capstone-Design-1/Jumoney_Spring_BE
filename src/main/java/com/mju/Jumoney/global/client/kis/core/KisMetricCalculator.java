package com.mju.Jumoney.global.client.kis.core;

import com.mju.Jumoney.global.client.kis.dto.dividend.KisDividendMetrics;
import com.mju.Jumoney.global.client.kis.dto.finance.KisFinancialRatioMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Component
public class KisMetricCalculator {

    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);
    private static final int RATIO_SCALE = 4;

    public BigDecimal calculatePayoutRatio(List<KisDividendMetrics> dividends,
                                           KisFinancialRatioMetrics financialRatio) {
        if (financialRatio == null) {
            return null;
        }

        if (dividends == null || dividends.isEmpty()) {
            return null;
        }

        List<BigDecimal> dpsValues = dividends.stream()
                .map(KisDividendMetrics::cashDividendPerShare)
                .filter(Objects::nonNull)
                .toList();

        if (dpsValues.isEmpty()) {
            return null;
        }

        BigDecimal totalDps = dpsValues.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return calculatePayoutRatio(totalDps, financialRatio.eps());
    }

    public BigDecimal calculatePayoutRatio(BigDecimal dps, BigDecimal eps) {
        if (dps == null || eps == null || eps.signum() <= 0) {
            return null;
        }

        return dps.multiply(PERCENT)
                .divide(eps, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
