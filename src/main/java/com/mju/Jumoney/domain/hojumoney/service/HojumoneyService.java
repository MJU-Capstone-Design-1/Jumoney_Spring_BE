package com.mju.Jumoney.domain.hojumoney.service;

import com.mju.Jumoney.domain.hojumoney.domain.HojumoneyPersona;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyIndicatorCandidate;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyRecommendationCandidate;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyRecommendationRequest;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyRecommendationResponse;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneyRiskCandidate;
import com.mju.Jumoney.domain.hojumoney.dto.HojumoneySurveySelection;
import com.mju.Jumoney.domain.hojumoney.enums.HojumoneyRecommendationTag;
import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.hojumoney.exception.HojumoneyErrorCode;
import com.mju.Jumoney.domain.hojumoney.repository.HojumoneyPersonaRepository;
import com.mju.Jumoney.domain.sector.service.GoodSectorService;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import com.mju.Jumoney.domain.stock.repository.HtsStockRepository;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HojumoneyService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RATIO_SCALE = 4;

    private static final int CAPITAL_PROTECTION_MARKET_CAP_LIMIT = 50;
    private static final BigDecimal CAPITAL_PROTECTION_MAX_DEBT_RATIO = BigDecimal.valueOf(100);
    private static final BigDecimal DIVIDEND_MIN_YIELD = BigDecimal.valueOf(4);
    private static final BigDecimal DIVIDEND_MAX_YIELD = BigDecimal.valueOf(8);
    private static final BigDecimal DIVIDEND_MIN_PAYOUT_RATIO = BigDecimal.valueOf(25);
    private static final BigDecimal DIVIDEND_MAX_PAYOUT_RATIO = BigDecimal.valueOf(80);
    private static final BigDecimal STEADY_GROWTH_MIN_ROE = BigDecimal.valueOf(8);
    private static final BigDecimal STEADY_GROWTH_MIN_EPS_GROWTH_RATE = BigDecimal.valueOf(5);
    private static final BigDecimal CAPITAL_GAIN_MAX_PER = BigDecimal.valueOf(10);
    private static final BigDecimal CAPITAL_GAIN_MAX_PBR = BigDecimal.ONE;

    private final HojumoneySurveySelectionService hojumoneySurveySelectionService;
    private final HojumoneyPersonaRepository hojumoneyPersonaRepository;
    private final HtsStockRepository htsStockRepository;
    private final StockIndicatorRepository stockIndicatorRepository;
    private final StockCurrentPriceService stockCurrentPriceService;
    private final GoodSectorService goodSectorService;

    public HojumoneyRecommendationResponse recommend(HojumoneyRecommendationRequest request) {
        HojumoneySurveySelection selection = hojumoneySurveySelectionService.validateAndClassify(request.selectedOptionIds());
        HojumoneyPersona persona = findPersona(selection);

        List<HojumoneyIndicatorCandidate> indicatorCandidates = findIndicatorCandidates(selection.investmentPurpose());
        List<HojumoneyRiskCandidate> riskCandidates = findRiskCandidates(selection.riskProfile());

        Map<Long, HojumoneyRecommendationCandidate> candidatesByStockId = new LinkedHashMap<>();
        for (HojumoneyIndicatorCandidate indicatorCandidate : indicatorCandidates) {
            HojumoneyRecommendationCandidate candidate = candidatesByStockId.computeIfAbsent(
                    indicatorCandidate.stock().getId(),
                    stockId -> new HojumoneyRecommendationCandidate(indicatorCandidate.stock())
            );
            candidate.setIndicator(indicatorCandidate.indicator());
            candidate.addTag(HojumoneyRecommendationTag.INDICATOR_MATCH);
        }
        for (HojumoneyRiskCandidate riskCandidate : riskCandidates) {
            HojumoneyRecommendationCandidate candidate = candidatesByStockId.computeIfAbsent(
                    riskCandidate.stock().getId(),
                    stockId -> new HojumoneyRecommendationCandidate(riskCandidate.stock())
            );
            candidate.addTag(HojumoneyRecommendationTag.RISK_PROFILE_MATCH);
        }

        attachMissingIndicators(candidatesByStockId);
        populateSortMetricValues(candidatesByStockId.values(), selection.investmentHorizon());

        String sortMetricKey = sortMetricKey(selection.investmentHorizon());
        Set<String> goodSectorNames = goodSectorService.getTodayGoodSectorNames();
        List<HojumoneyRecommendationCandidate> eligibleCandidates = candidatesByStockId.values().stream()
                .filter(candidate -> candidate.getIndicator() != null)
                .filter(candidate -> candidate.getSortMetricValue() != null)
                .sorted(candidateComparator(selection.investmentHorizon(), goodSectorNames))
                .toList();

        List<HojumoneyRecommendationCandidate> topCandidates = eligibleCandidates.stream()
                .limit(DEFAULT_RECOMMENDATION_LIMIT)
                .toList();
        Map<String, StockCurrentPriceSnapshot> currentPrices = stockCurrentPriceService.getCurrentPrices(
                topCandidates.stream()
                        .map(candidate -> candidate.getStock().getStockCode())
                        .toList()
        );

        List<HojumoneyRecommendationResponse.RecommendedStockResponse> recommendations = topCandidates.stream()
                .map(candidate -> toRecommendedStockResponse(
                        candidate,
                        selection.investmentPurpose(),
                        selection.riskProfile(),
                        sortMetricKey,
                        currentPrices.get(candidate.getStock().getStockCode()),
                        goodSectorNames
                ))
                .toList();

        return new HojumoneyRecommendationResponse(
                null,
                selection.investmentPurpose(),
                selection.riskProfile(),
                selection.investmentHorizon(),
                toPersonaResponse(persona),
                eligibleCandidates.size(),
                rank(recommendations)
        );
    }

    private HojumoneyPersona findPersona(HojumoneySurveySelection selection) {
        return hojumoneyPersonaRepository.findByInvestmentPurposeAndRiskProfileAndInvestmentHorizon(
                        selection.investmentPurpose(),
                        selection.riskProfile(),
                        selection.investmentHorizon()
                )
                .orElseThrow(() -> new CustomException(
                        HojumoneyErrorCode.HOJUMONEY_PERSONA_NOT_FOUND,
                        "investmentPurpose=" + selection.investmentPurpose()
                                + ", riskProfile=" + selection.riskProfile()
                                + ", investmentHorizon=" + selection.investmentHorizon()
                ));
    }

    private HojumoneyRecommendationResponse.HojumoneyPersonaResponse toPersonaResponse(HojumoneyPersona persona) {
        return new HojumoneyRecommendationResponse.HojumoneyPersonaResponse(
                persona.getPersonaName(),
                persona.getPersonaDescription()
        );
    }

    private List<HojumoneyIndicatorCandidate> findIndicatorCandidates(SurveyLogicCode investmentPurpose) {
        String baseTime = stockIndicatorRepository.findLatestBaseTime()
                .orElseThrow(() -> new CustomException(HojumoneyErrorCode.STOCK_INDICATOR_BASE_TIME_NOT_FOUND));

        List<StockIndicator> indicators = switch (investmentPurpose) {
            case CAPITAL_PROTECTION -> findCapitalProtectionCandidates(baseTime);
            case DIVIDEND_INCOME -> stockIndicatorRepository.findDividendIncomeCandidates(
                    baseTime,
                    DIVIDEND_MIN_YIELD,
                    DIVIDEND_MAX_YIELD,
                    DIVIDEND_MIN_PAYOUT_RATIO,
                    DIVIDEND_MAX_PAYOUT_RATIO
            );
            case STEADY_GROWTH -> stockIndicatorRepository.findSteadyGrowthCandidates(
                    baseTime,
                    STEADY_GROWTH_MIN_ROE,
                    STEADY_GROWTH_MIN_EPS_GROWTH_RATE
            );
            case CAPITAL_GAIN -> stockIndicatorRepository.findCapitalGainCandidates(
                    baseTime,
                    CAPITAL_GAIN_MAX_PER,
                    CAPITAL_GAIN_MAX_PBR
            );
            default -> throw new CustomException(HojumoneyErrorCode.INVALID_HOJUMONEY_LOGIC_CODE);
        };

        return indicators.stream()
                .map(HojumoneyIndicatorCandidate::from)
                .toList();
    }

    private List<StockIndicator> findCapitalProtectionCandidates(String baseTime) {
        List<Long> topMarketCapStockIds = stockIndicatorRepository.findTopMarketCapStockIds(
                baseTime,
                PageRequest.of(0, CAPITAL_PROTECTION_MARKET_CAP_LIMIT)
        );
        if (topMarketCapStockIds.isEmpty()) {
            return List.of();
        }

        return stockIndicatorRepository.findCapitalProtectionCandidatesInTopMarketCapStocks(
                baseTime,
                topMarketCapStockIds,
                CAPITAL_PROTECTION_MAX_DEBT_RATIO
        );
    }

    private List<HojumoneyRiskCandidate> findRiskCandidates(SurveyLogicCode riskProfile) {
        HtsSearchType searchType = toHtsSearchType(riskProfile);
        LocalDate baseDate = htsStockRepository.findLatestBaseDateBySearchType(searchType)
                .orElseThrow(() -> new CustomException(HojumoneyErrorCode.HTS_STOCK_BASE_DATE_NOT_FOUND));

        return htsStockRepository.findBySearchTypeAndBaseDateWithStock(searchType, baseDate)
                .stream()
                .map(HojumoneyRiskCandidate::from)
                .toList();
    }

    private HtsSearchType toHtsSearchType(SurveyLogicCode riskProfile) {
        return switch (riskProfile) {
            case STABILITY -> HtsSearchType.STABILITY;
            case SAFE_PURSUIT -> HtsSearchType.SAFE_PURSUIT;
            case PROFIT_PURSUIT -> HtsSearchType.PROFIT_PURSUIT;
            case AGGRESSIVE -> HtsSearchType.AGGRESSIVE;
            default -> throw new CustomException(HojumoneyErrorCode.INVALID_HOJUMONEY_LOGIC_CODE);
        };
    }

    private void attachMissingIndicators(Map<Long, HojumoneyRecommendationCandidate> candidatesByStockId) {
        List<Long> missingIndicatorStockIds = candidatesByStockId.values().stream()
                .filter(candidate -> candidate.getIndicator() == null)
                .map(candidate -> candidate.getStock().getId())
                .toList();
        if (missingIndicatorStockIds.isEmpty()) {
            return;
        }

        String baseTime = stockIndicatorRepository.findLatestBaseTime()
                .orElseThrow(() -> new CustomException(HojumoneyErrorCode.STOCK_INDICATOR_BASE_TIME_NOT_FOUND));
        stockIndicatorRepository.findByBaseTimeAndStockIdsWithStock(baseTime, missingIndicatorStockIds)
                .forEach(indicator -> {
                    HojumoneyRecommendationCandidate candidate = candidatesByStockId.get(indicator.getStock().getId());
                    if (candidate != null) {
                        candidate.setIndicator(indicator);
                    }
                });
    }

    private void populateSortMetricValues(
            Collection<HojumoneyRecommendationCandidate> candidates,
            SurveyLogicCode investmentHorizon
    ) {
        candidates.stream()
                .filter(candidate -> candidate.getIndicator() != null)
                .forEach(candidate -> candidate.setSortMetricValue(sortMetricValue(candidate, investmentHorizon)));
    }

    private Comparator<HojumoneyRecommendationCandidate> candidateComparator(
            SurveyLogicCode investmentHorizon,
            Set<String> goodSectorNames
    ) {
        Comparator<HojumoneyRecommendationCandidate> comparator = Comparator
                .comparingInt(HojumoneyRecommendationCandidate::matchedConditionCount)
                .reversed()
                .thenComparing(candidate -> goodSectorService.hasGoodSectorMatch(candidate.getStock(), goodSectorNames), Comparator.reverseOrder());

        Comparator<HojumoneyRecommendationCandidate> sortMetricComparator = Comparator.comparing(
                HojumoneyRecommendationCandidate::getSortMetricValue,
                Comparator.nullsLast(BigDecimal::compareTo)
        );

        return switch (investmentHorizon) {
            case ULTRA_SHORT, SHORT, MID, LONG -> comparator
                    .thenComparing(sortMetricComparator.reversed())
                    .thenComparing(candidate -> candidate.getStock().getStockCode());
            default -> throw new CustomException(HojumoneyErrorCode.INVALID_HOJUMONEY_LOGIC_CODE);
        };
    }

    private BigDecimal sortMetricValue(HojumoneyRecommendationCandidate candidate, SurveyLogicCode investmentHorizon) {
        StockIndicator indicator = candidate.getIndicator();
        return switch (investmentHorizon) {
            case ULTRA_SHORT -> requiredIndicatorMetric(
                    indicator,
                    candidate.getStock(),
                    "executionStrength",
                    indicator.getExecutionStrength()
            );
            case SHORT -> BigDecimal.valueOf(requiredLongIndicatorMetric(
                    indicator,
                    candidate.getStock(),
                    "accumulatedTradeAmount"
            ));
            case MID -> epsGrowthRate(indicator);
            case LONG -> requiredIndicatorMetric(indicator, candidate.getStock(), "roe", indicator.getRoe());
            default -> throw new CustomException(HojumoneyErrorCode.INVALID_HOJUMONEY_LOGIC_CODE);
        };
    }

    private Long requiredLongIndicatorMetric(StockIndicator indicator, Stock stock, String fieldName) {
        Long value = switch (fieldName) {
            case "accumulatedTradeAmount" -> indicator.getAccumulatedTradeAmount();
            default -> throw new CustomException(HojumoneyErrorCode.INVALID_HOJUMONEY_LOGIC_CODE);
        };
        if (value == null) {
            throwMissingIndicatorMetric(stock, indicator.getBaseTime(), fieldName);
        }
        return value;
    }

    private BigDecimal requiredIndicatorMetric(
            StockIndicator indicator,
            Stock stock,
            String fieldName,
            BigDecimal value
    ) {
        if (value == null) {
            throwMissingIndicatorMetric(stock, indicator.getBaseTime(), fieldName);
        }
        return value;
    }

    private void throwMissingIndicatorMetric(Stock stock, String baseTime, String fieldName) {
        throw new CustomException(
                HojumoneyErrorCode.STOCK_INDICATOR_REQUIRED_METRIC_MISSING,
                "stockCode=" + stock.getStockCode()
                        + ", stockName=" + stock.getName()
                        + ", baseTime=" + baseTime
                        + ", field=" + fieldName
        );
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

    private String sortMetricKey(SurveyLogicCode investmentHorizon) {
        return switch (investmentHorizon) {
            case ULTRA_SHORT -> "EXECUTION_STRENGTH";
            case SHORT -> "ACCUMULATED_TRADE_AMOUNT";
            case MID -> "EPS_GROWTH_RATE";
            case LONG -> "ROE";
            default -> throw new CustomException(HojumoneyErrorCode.INVALID_HOJUMONEY_LOGIC_CODE);
        };
    }

    private HojumoneyRecommendationResponse.RecommendedStockResponse toRecommendedStockResponse(
            HojumoneyRecommendationCandidate candidate,
            SurveyLogicCode investmentPurpose,
            SurveyLogicCode riskProfile,
            String sortMetricKey,
            StockCurrentPriceSnapshot currentPrice,
            Set<String> goodSectorNames
    ) {
        Stock stock = candidate.getStock();
        List<String> goodSectorTags = goodSectorService.goodSectorTags(stock, goodSectorNames);
        return new HojumoneyRecommendationResponse.RecommendedStockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                0,
                responseTags(candidate, investmentPurpose, riskProfile),
                goodSectorTags,
                candidate.matchedConditionCount(),
                sortMetricKey,
                candidate.getSortMetricValue(),
                currentPrice == null ? null : currentPrice.currentPrice(),
                currentPrice == null ? null : currentPrice.changeRate()
        );
    }

    private List<SurveyLogicCode> responseTags(
            HojumoneyRecommendationCandidate candidate,
            SurveyLogicCode investmentPurpose,
            SurveyLogicCode riskProfile
    ) {
        List<SurveyLogicCode> tags = new ArrayList<>();
        if (candidate.getTags().contains(HojumoneyRecommendationTag.INDICATOR_MATCH)) {
            tags.add(investmentPurpose);
        }
        if (candidate.getTags().contains(HojumoneyRecommendationTag.RISK_PROFILE_MATCH)) {
            tags.add(riskProfile);
        }
        return tags;
    }

    private List<HojumoneyRecommendationResponse.RecommendedStockResponse> rank(
            List<HojumoneyRecommendationResponse.RecommendedStockResponse> recommendations
    ) {
        List<HojumoneyRecommendationResponse.RecommendedStockResponse> ranked = new ArrayList<>();
        for (int i = 0; i < recommendations.size(); i++) {
            HojumoneyRecommendationResponse.RecommendedStockResponse item = recommendations.get(i);
            ranked.add(new HojumoneyRecommendationResponse.RecommendedStockResponse(
                    item.stockId(),
                    item.stockCode(),
                    item.stockName(),
                    i + 1,
                    item.tags(),
                    item.goodSectorTags(),
                    item.matchedConditionCount(),
                    item.sortMetricKey(),
                    item.sortMetricValue(),
                    item.currentPrice(),
                    item.changeRate()
            ));
        }
        return ranked;
    }
}
