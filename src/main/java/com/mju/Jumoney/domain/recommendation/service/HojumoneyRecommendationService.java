package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.recommendation.domain.HojumoneyPersona;
import com.mju.Jumoney.domain.recommendation.dto.*;
import com.mju.Jumoney.domain.recommendation.enums.HojumoneyRecommendationTag;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.recommendation.repository.HojumoneyPersonaRepository;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.domain.stock.service.StockCurrentPriceService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HojumoneyRecommendationService {

    private static final int DEFAULT_RECOMMENDATION_LIMIT = 10;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RATIO_SCALE = 4;

    private final HojumoneySurveySelectionService surveySelectionService;
    private final HojumoneyIndicatorFilterService indicatorFilterService;
    private final HojumoneyRiskFilterService riskFilterService;
    private final HojumoneyPersonaRepository hojumoneyPersonaRepository;
    private final StockIndicatorRepository stockIndicatorRepository;
    private final StockCurrentPriceService stockCurrentPriceService;

    public HojumoneyRecommendationResponse recommend(HojumoneyRecommendationRequest request) {
        HojumoneySurveySelection selection = surveySelectionService.validateAndClassify(request.selectedOptionIds());
        HojumoneyPersona persona = findPersona(selection);

        List<HojumoneyIndicatorCandidate> indicatorCandidates = indicatorFilterService.findCandidates(selection.investmentPurpose());
        List<HojumoneyRiskCandidate> riskCandidates = riskFilterService.findCandidates(selection.riskProfile());

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
        List<HojumoneyRecommendationCandidate> eligibleCandidates = candidatesByStockId.values().stream()
                .filter(candidate -> candidate.getIndicator() != null)
                .filter(candidate -> candidate.getSortMetricValue() != null)
                .sorted(candidateComparator(selection.investmentHorizon()))
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
                        currentPrices.get(candidate.getStock().getStockCode())
                ))
                .toList();

        return new HojumoneyRecommendationResponse(
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
                        RecommendationErrorCode.HOJUMONEY_PERSONA_NOT_FOUND,
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

    private void attachMissingIndicators(Map<Long, HojumoneyRecommendationCandidate> candidatesByStockId) {
        List<Long> missingIndicatorStockIds = candidatesByStockId.values().stream()
                .filter(candidate -> candidate.getIndicator() == null)
                .map(candidate -> candidate.getStock().getId())
                .toList();
        if (missingIndicatorStockIds.isEmpty()) {
            return;
        }

        String baseTime = stockIndicatorRepository.findLatestBaseTime()
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.STOCK_INDICATOR_BASE_TIME_NOT_FOUND));
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

    private Comparator<HojumoneyRecommendationCandidate> candidateComparator(SurveyLogicCode investmentHorizon) {
        Comparator<HojumoneyRecommendationCandidate> comparator = Comparator
                .comparingInt(HojumoneyRecommendationCandidate::matchedConditionCount)
                .reversed();

        Comparator<HojumoneyRecommendationCandidate> sortMetricComparator = Comparator.comparing(
                HojumoneyRecommendationCandidate::getSortMetricValue,
                Comparator.nullsLast(BigDecimal::compareTo)
        );

        return switch (investmentHorizon) {
            case ULTRA_SHORT, SHORT, MID, LONG -> comparator
                    .thenComparing(sortMetricComparator.reversed())
                    .thenComparing(candidate -> candidate.getStock().getStockCode());
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
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
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
        };
    }

    private Long requiredLongIndicatorMetric(StockIndicator indicator, Stock stock, String fieldName) {
        Long value = switch (fieldName) {
            case "accumulatedTradeAmount" -> indicator.getAccumulatedTradeAmount();
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
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
                RecommendationErrorCode.STOCK_INDICATOR_REQUIRED_METRIC_MISSING,
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
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
        };
    }

    private HojumoneyRecommendationResponse.RecommendedStockResponse toRecommendedStockResponse(
            HojumoneyRecommendationCandidate candidate,
            SurveyLogicCode investmentPurpose,
            SurveyLogicCode riskProfile,
            String sortMetricKey,
            StockCurrentPriceSnapshot currentPrice
    ) {
        Stock stock = candidate.getStock();
        return new HojumoneyRecommendationResponse.RecommendedStockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                0,
                responseTags(candidate, investmentPurpose, riskProfile),
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
