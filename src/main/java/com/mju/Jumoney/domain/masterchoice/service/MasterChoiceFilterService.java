package com.mju.Jumoney.domain.masterchoice.service;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceCandidate;
import com.mju.Jumoney.domain.masterchoice.exception.MasterChoiceErrorCode;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterChoiceFilterService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RATIO_SCALE = 4;

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

    private final StockIndicatorRepository stockIndicatorRepository;

    public List<MasterChoiceCandidate> findCandidates(
            MasterCode masterCode,
            Collection<MasterOptionLogicCode> selectedLogicCodes,
            Collection<SectorType> selectedSectorTypes
    ) {
        Set<MasterOptionLogicCode> logicCodes = normalizeLogicCodes(masterCode, selectedLogicCodes);
        Set<SectorType> sectorTypes = selectedSectorTypes == null || selectedSectorTypes.isEmpty()
                ? Set.of()
                : EnumSet.copyOf(selectedSectorTypes);

        String baseTime = stockIndicatorRepository.findLatestBaseTime()
                .orElseThrow(() -> new CustomException(MasterChoiceErrorCode.STOCK_INDICATOR_BASE_TIME_NOT_FOUND));

        List<StockIndicator> indicators = stockIndicatorRepository.findByBaseTimeWithStockAndSector(baseTime);
        List<MasterChoiceCandidate> candidates = new ArrayList<>();
        for (StockIndicator indicator : indicators) {
            MasterChoiceCandidate candidate = evaluate(indicator, logicCodes, sectorTypes);
            if (candidate.matchedConditionCount() == logicCodes.size()) {
                candidate.setSortMetricValue(sortMetricValue(masterCode, indicator));
                if (masterCode == MasterCode.PETER_LYNCH) {
                    candidate.setFallbackSortMetricValue(indicator.getSalesGrowthRate());
                }
                candidates.add(candidate);
            }
        }
        return candidates;
    }

    private Set<MasterOptionLogicCode> normalizeLogicCodes(
            MasterCode masterCode,
            Collection<MasterOptionLogicCode> selectedLogicCodes
    ) {
        Set<MasterOptionLogicCode> logicCodes = selectedLogicCodes == null || selectedLogicCodes.isEmpty()
                ? defaultLogicCodes(masterCode)
                : EnumSet.copyOf(selectedLogicCodes);

        boolean hasInvalidCode = logicCodes.stream()
                .anyMatch(logicCode -> logicCode.getMasterCode() != masterCode);
        if (hasInvalidCode) {
            throw new CustomException(MasterChoiceErrorCode.INVALID_MASTER_OPTION_SELECTION);
        }
        return logicCodes;
    }

    private Set<MasterOptionLogicCode> defaultLogicCodes(MasterCode masterCode) {
        EnumSet<MasterOptionLogicCode> logicCodes = EnumSet.noneOf(MasterOptionLogicCode.class);
        Arrays.stream(MasterOptionLogicCode.values())
                .filter(logicCode -> logicCode.getMasterCode() == masterCode)
                .forEach(logicCodes::add);
        return logicCodes;
    }

    private MasterChoiceCandidate evaluate(
            StockIndicator indicator,
            Set<MasterOptionLogicCode> logicCodes,
            Set<SectorType> selectedSectorTypes
    ) {
        MasterChoiceCandidate candidate = new MasterChoiceCandidate(indicator);
        for (MasterOptionLogicCode logicCode : logicCodes) {
            if (matches(indicator, logicCode, selectedSectorTypes)) {
                candidate.addMatchedOption(logicCode);
            }
        }
        return candidate;
    }

    private boolean matches(
            StockIndicator indicator,
            MasterOptionLogicCode logicCode,
            Set<SectorType> selectedSectorTypes
    ) {
        return switch (logicCode) {
            case BUFFETT_ROE -> greaterThanOrEqual(indicator.getRoe(), BUFFETT_MIN_ROE);
            case BUFFETT_PER -> positive(indicator.getPer()) && lessThanOrEqual(indicator.getPer(), BUFFETT_MAX_PER);
            case BUFFETT_EPS_GROWTH -> greaterThanOrEqual(epsGrowthRate(indicator), BUFFETT_MIN_EPS_GROWTH_RATE);
            case BUFFETT_DEBT_RATIO -> lessThanOrEqual(indicator.getDebtRatio(), BUFFETT_MAX_DEBT_RATIO);
            case BUFFETT_OPERATING_MARGIN -> greaterThanOrEqual(operatingMargin(indicator), BUFFETT_MIN_OPERATING_MARGIN);

            case LYNCH_PEG -> lessThanOrEqual(peg(indicator), LYNCH_MAX_PEG);
            case LYNCH_EPS_GROWTH -> between(epsGrowthRate(indicator), LYNCH_MIN_EPS_GROWTH_RATE, LYNCH_MAX_EPS_GROWTH_RATE);
            case LYNCH_DEBT_RATIO -> lessThanOrEqual(indicator.getDebtRatio(), LYNCH_MAX_DEBT_RATIO);
            case LYNCH_SALES_GROWTH -> greaterThanOrEqual(indicator.getSalesGrowthRate(), LYNCH_MIN_SALES_GROWTH_RATE);
            case LYNCH_SECTOR -> matchesSelectedSector(indicator, selectedSectorTypes);

            case DALIO_ALL_WEATHER -> matchesSelectedSector(indicator, selectedSectorTypes);
            case DALIO_PER -> positive(indicator.getPer()) && lessThanOrEqual(indicator.getPer(), DALIO_MAX_PER);
            case DALIO_MARGIN_DEBT -> lessThanOrEqual(indicator.getMarginDebtRate(), DALIO_MAX_MARGIN_DEBT_RATE);
            case DALIO_DEBT_RATIO -> lessThanOrEqual(indicator.getDebtRatio(), DALIO_MAX_DEBT_RATIO);
            case DALIO_EARNINGS_YIELD -> greaterThanOrEqual(earningsYield(indicator), DALIO_MIN_EARNINGS_YIELD);

            case ONEIL_EPS_GROWTH -> greaterThanOrEqual(epsGrowthRate(indicator), ONEIL_MIN_EPS_GROWTH_RATE);
            case ONEIL_ROE -> greaterThanOrEqual(indicator.getRoe(), ONEIL_MIN_ROE);
            case ONEIL_HIGH_52_WEEK -> greaterThanOrEqual(indicator.getHigh52WeekRate(), ONEIL_MIN_HIGH_52_WEEK_RATE);
            case ONEIL_MARKET_LEADER -> indicator.getStock().isMarketLeader();
            case ONEIL_INST_NET_BUY -> indicator.getInstNetBuy20Days() != null && indicator.getInstNetBuy20Days() >= 0;
        };
    }

    private BigDecimal sortMetricValue(MasterCode masterCode, StockIndicator indicator) {
        return switch (masterCode) {
            case WARREN_BUFFETT -> indicator.getRoe();
            case PETER_LYNCH -> peg(indicator);
            case RAY_DALIO -> indicator.getMarketCap() == null ? null : BigDecimal.valueOf(indicator.getMarketCap());
            case WILLIAM_ONEIL -> indicator.getHigh52WeekRate();
        };
    }

    private boolean matchesSelectedSector(StockIndicator indicator, Set<SectorType> selectedSectorTypes) {
        return !selectedSectorTypes.isEmpty()
                && selectedSectorTypes.contains(indicator.getStock().getSector().getSectorName());
    }

    private BigDecimal epsGrowthRate(StockIndicator indicator) {
        if (indicator.getCurrentEps() == null
                || indicator.getLastYearEps() == null
                || indicator.getLastYearEps().signum() <= 0) {
            return null;
        }
        return indicator.getCurrentEps()
                .subtract(indicator.getLastYearEps())
                .multiply(HUNDRED)
                .divide(indicator.getLastYearEps(), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal operatingMargin(StockIndicator indicator) {
        if (indicator.getOperatingProfit() == null || indicator.getCurrentSales() == null || indicator.getCurrentSales() <= 0) {
            return null;
        }
        return BigDecimal.valueOf(indicator.getOperatingProfit())
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(indicator.getCurrentSales()), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal peg(StockIndicator indicator) {
        BigDecimal epsGrowthRate = epsGrowthRate(indicator);
        if (indicator.getPer() == null || indicator.getPer().signum() <= 0 || epsGrowthRate == null || epsGrowthRate.signum() <= 0) {
            return null;
        }
        return indicator.getPer().divide(epsGrowthRate, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal earningsYield(StockIndicator indicator) {
        if (indicator.getPer() == null || indicator.getPer().signum() <= 0) {
            return null;
        }
        return HUNDRED.divide(indicator.getPer(), RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private boolean between(BigDecimal value, BigDecimal min, BigDecimal max) {
        return greaterThanOrEqual(value, min) && lessThanOrEqual(value, max);
    }

    private boolean greaterThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) >= 0;
    }

    private boolean lessThanOrEqual(BigDecimal value, BigDecimal threshold) {
        return value != null && value.compareTo(threshold) <= 0;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
