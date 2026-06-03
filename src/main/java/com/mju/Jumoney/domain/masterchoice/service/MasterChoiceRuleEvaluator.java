package com.mju.Jumoney.domain.masterchoice.service;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

final class MasterChoiceRuleEvaluator {

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    static final int RATIO_SCALE = 4;

    private static final BigDecimal BUFFETT_MIN_ROE = BigDecimal.valueOf(15);
    private static final BigDecimal BUFFETT_MAX_PER = BigDecimal.valueOf(15);
    private static final BigDecimal BUFFETT_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(10);
    private static final BigDecimal BUFFETT_MAX_DEBT_RATIO = BigDecimal.valueOf(100);
    private static final BigDecimal BUFFETT_MIN_OPERATING_MARGIN = BigDecimal.valueOf(20);

    private static final BigDecimal LYNCH_MAX_PEG = BigDecimal.ONE;
    private static final BigDecimal LYNCH_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(20);
    private static final BigDecimal LYNCH_MAX_EPS_GROWTH_RATE = BigDecimal.valueOf(50);
    private static final BigDecimal LYNCH_MAX_DEBT_RATIO = BigDecimal.valueOf(100);
    private static final BigDecimal LYNCH_MIN_SALES_GROWTH_RATE = BigDecimal.valueOf(10);

    private static final BigDecimal DALIO_MAX_PER = BigDecimal.valueOf(20);
    private static final BigDecimal DALIO_MAX_MARGIN_DEBT_RATE = BigDecimal.valueOf(5);
    private static final BigDecimal DALIO_MAX_DEBT_RATIO = BigDecimal.valueOf(50);
    private static final BigDecimal DALIO_MIN_EARNINGS_YIELD = BigDecimal.valueOf(3.38);

    private static final BigDecimal ONEIL_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(25);
    private static final BigDecimal ONEIL_MIN_ROE = BigDecimal.valueOf(17);
    private static final BigDecimal ONEIL_MIN_HIGH_52_WEEK_RATE = BigDecimal.valueOf(90);

    private MasterChoiceRuleEvaluator() {
    }

    static Indicator fromStockIndicator(StockIndicator indicator) {
        BigDecimal epsGrowthRate = epsGrowthRate(indicator.getCurrentEps(), indicator.getLastYearEps());
        BigDecimal operatingMargin = operatingMargin(indicator.getOperatingProfit(), indicator.getCurrentSales());
        BigDecimal peg = peg(indicator.getPer(), epsGrowthRate);
        BigDecimal earningsYield = earningsYield(indicator.getPer());

        return new Indicator(
                indicator.getStock(),
                indicator.getMarketCap(),
                indicator.getRoe(),
                indicator.getPer(),
                epsGrowthRate,
                indicator.getDebtRatio(),
                operatingMargin,
                indicator.getSalesGrowthRate(),
                indicator.getMarginDebtRate(),
                indicator.getHigh52WeekRate(),
                indicator.getInstNetBuy20Days(),
                peg,
                earningsYield
        );
    }

    static Indicator fromBacktestValues(Stock stock,
                                        BigDecimal roe,
                                        BigDecimal per,
                                        BigDecimal currentEps,
                                        BigDecimal lastYearEps,
                                        BigDecimal debtRatio,
                                        Long operatingProfit,
                                        Long currentSales,
                                        BigDecimal salesGrowthRate,
                                        BigDecimal marginDebtRate,
                                        BigDecimal high52WeekRate,
                                        Long instNetBuy20Days) {
        BigDecimal epsGrowthRate = epsGrowthRate(currentEps, lastYearEps);
        BigDecimal operatingMargin = operatingMargin(operatingProfit, currentSales);
        BigDecimal peg = peg(per, epsGrowthRate);
        BigDecimal earningsYield = earningsYield(per);

        return new Indicator(
                stock,
                null,
                roe,
                per,
                epsGrowthRate,
                debtRatio,
                operatingMargin,
                salesGrowthRate,
                marginDebtRate,
                high52WeekRate,
                instNetBuy20Days,
                peg,
                earningsYield
        );
    }

    static boolean matches(Indicator indicator,
                           MasterOptionLogicCode logicCode,
                           Set<SectorType> selectedSectorTypes) {
        return switch (logicCode) {
            case BUFFETT_ROE -> greaterThanOrEqual(indicator.roe(), BUFFETT_MIN_ROE);
            case BUFFETT_PER -> positive(indicator.per()) && lessThanOrEqual(indicator.per(), BUFFETT_MAX_PER);
            case BUFFETT_EPS_GROWTH -> greaterThanOrEqual(indicator.epsGrowthRate(), BUFFETT_MIN_EPS_GROWTH_RATE);
            case BUFFETT_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), BUFFETT_MAX_DEBT_RATIO);
            case BUFFETT_OPERATING_MARGIN ->
                    greaterThanOrEqual(indicator.operatingMargin(), BUFFETT_MIN_OPERATING_MARGIN);

            case LYNCH_PEG -> lessThanOrEqual(indicator.peg(), LYNCH_MAX_PEG);
            case LYNCH_EPS_GROWTH ->
                    between(indicator.epsGrowthRate(), LYNCH_MIN_EPS_GROWTH_RATE, LYNCH_MAX_EPS_GROWTH_RATE);
            case LYNCH_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), LYNCH_MAX_DEBT_RATIO);
            case LYNCH_SALES_GROWTH -> greaterThanOrEqual(indicator.salesGrowthRate(), LYNCH_MIN_SALES_GROWTH_RATE);
            case LYNCH_SECTOR -> matchesSelectedSector(indicator.stock(), selectedSectorTypes);

            case DALIO_ALL_WEATHER -> matchesSelectedSector(indicator.stock(), selectedSectorTypes);
            case DALIO_PER -> positive(indicator.per()) && lessThanOrEqual(indicator.per(), DALIO_MAX_PER);
            case DALIO_MARGIN_DEBT -> lessThanOrEqual(indicator.marginDebtRate(), DALIO_MAX_MARGIN_DEBT_RATE);
            case DALIO_DEBT_RATIO -> lessThanOrEqual(indicator.debtRatio(), DALIO_MAX_DEBT_RATIO);
            case DALIO_EARNINGS_YIELD -> greaterThanOrEqual(indicator.earningsYield(), DALIO_MIN_EARNINGS_YIELD);

            case ONEIL_EPS_GROWTH -> greaterThanOrEqual(indicator.epsGrowthRate(), ONEIL_MIN_EPS_GROWTH_RATE);
            case ONEIL_ROE -> greaterThanOrEqual(indicator.roe(), ONEIL_MIN_ROE);
            case ONEIL_HIGH_52_WEEK -> greaterThanOrEqual(indicator.high52WeekRate(), ONEIL_MIN_HIGH_52_WEEK_RATE);
            case ONEIL_MARKET_LEADER -> indicator.stock().isMarketLeader();
            case ONEIL_INST_NET_BUY -> indicator.instNetBuy20Days() != null && indicator.instNetBuy20Days() >= 0;
        };
    }

    static BigDecimal sortMetricValue(MasterCode masterCode, Indicator indicator) {
        return switch (masterCode) {
            case WARREN_BUFFETT -> indicator.roe();
            case PETER_LYNCH -> indicator.peg();
            case RAY_DALIO -> indicator.marketCap() == null ? null : BigDecimal.valueOf(indicator.marketCap());
            case WILLIAM_ONEIL -> indicator.high52WeekRate();
        };
    }

    static BigDecimal per(BigDecimal price, BigDecimal eps) {
        if (price == null || eps == null || eps.signum() <= 0) {
            return null;
        }
        return price.divide(eps, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal epsGrowthRate(BigDecimal currentEps, BigDecimal lastYearEps) {
        if (currentEps == null || lastYearEps == null || lastYearEps.signum() <= 0) {
            return null;
        }
        return currentEps.subtract(lastYearEps)
                .multiply(HUNDRED)
                .divide(lastYearEps, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal operatingMargin(Long operatingProfit, Long currentSales) {
        if (operatingProfit == null || currentSales == null || currentSales <= 0) {
            return null;
        }
        return BigDecimal.valueOf(operatingProfit)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(currentSales), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal peg(BigDecimal per, BigDecimal epsGrowthRate) {
        if (per == null || per.signum() <= 0 || epsGrowthRate == null || epsGrowthRate.signum() <= 0) {
            return null;
        }
        return per.divide(epsGrowthRate, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal earningsYield(BigDecimal per) {
        if (per == null || per.signum() <= 0) {
            return null;
        }
        return HUNDRED.divide(per, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private static boolean matchesSelectedSector(Stock stock, Set<SectorType> selectedSectorTypes) {
        return !selectedSectorTypes.isEmpty()
                && selectedSectorTypes.contains(stock.getSector().getSectorName());
    }

    private static boolean greaterThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) >= 0;
    }

    private static boolean lessThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) <= 0;
    }

    private static boolean between(BigDecimal value, BigDecimal min, BigDecimal max) {
        return greaterThanOrEqual(value, min) && lessThanOrEqual(value, max);
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    record Indicator(
            Stock stock,
            Long marketCap,
            BigDecimal roe,
            BigDecimal per,
            BigDecimal epsGrowthRate,
            BigDecimal debtRatio,
            BigDecimal operatingMargin,
            BigDecimal salesGrowthRate,
            BigDecimal marginDebtRate,
            BigDecimal high52WeekRate,
            Long instNetBuy20Days,
            BigDecimal peg,
            BigDecimal earningsYield
    ) {
    }
}
