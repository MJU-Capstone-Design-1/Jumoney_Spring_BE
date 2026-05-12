package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.recommendation.dto.*;
import com.mju.Jumoney.domain.recommendation.enums.HojumoneyRecommendationTag;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;
import com.mju.Jumoney.domain.stock.repository.StockIndicatorRepository;
import com.mju.Jumoney.domain.stock.service.StockExecutionStrengthService;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final StockIndicatorRepository stockIndicatorRepository;
    private final StockExecutionStrengthService stockExecutionStrengthService;

    public HojumoneyRecommendationResponse recommend(HojumoneyRecommendationRequest request) {
        HojumoneySurveySelection selection = surveySelectionService.validateAndClassify(request.selectedOptionIds());

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

        String sortMetricName = sortMetricName(selection.investmentHorizon());
        List<HojumoneyRecommendationCandidate> eligibleCandidates = candidatesByStockId.values().stream()
                .filter(candidate -> candidate.getIndicator() != null)
                .filter(candidate -> candidate.getSortMetricValue() != null)
                .sorted(candidateComparator(selection.investmentHorizon()))
                .toList();

        List<HojumoneyRecommendationResponse.RecommendedStockResponse> recommendations = eligibleCandidates.stream()
                .limit(DEFAULT_RECOMMENDATION_LIMIT)
                .map(candidate -> toRecommendedStockResponse(candidate, sortMetricName))
                .toList();

        return new HojumoneyRecommendationResponse(
                selection.investmentPurpose(),
                selection.riskProfile(),
                selection.investmentHorizon(),
                eligibleCandidates.size(),
                rank(recommendations)
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
        if (investmentHorizon == SurveyLogicCode.ULTRA_SHORT) {
            populateUltraShortSortMetricValues(candidates);
            return;
        }

        candidates.stream()
                .filter(candidate -> candidate.getIndicator() != null)
                .forEach(candidate -> candidate.setSortMetricValue(sortMetricValue(candidate, investmentHorizon)));
    }

    private void populateUltraShortSortMetricValues(Collection<HojumoneyRecommendationCandidate> candidates) {
        Map<String, BigDecimal> executionStrengths = stockExecutionStrengthService.getExecutionStrengths(
                candidates.stream()
                        .filter(candidate -> candidate.getIndicator() != null)
                        .map(candidate -> candidate.getStock().getStockCode())
                        .toList()
        );

        candidates.forEach(candidate -> candidate.setSortMetricValue(
                executionStrengths.get(candidate.getStock().getStockCode())
        ));
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
            case ULTRA_SHORT -> candidate.getSortMetricValue();
            case SHORT -> BigDecimal.valueOf(indicator.getAccumulatedTradeAmount());
            case MID -> epsGrowthRate(indicator);
            case LONG -> indicator.getRoe();
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
        };
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

    private String sortMetricName(SurveyLogicCode investmentHorizon) {
        return switch (investmentHorizon) {
            case ULTRA_SHORT -> "체결강도";
            case SHORT -> "거래대금";
            case MID -> "EPS 성장률";
            case LONG -> "ROE";
            default -> throw new CustomException(RecommendationErrorCode.INVALID_RECOMMENDATION_LOGIC_CODE);
        };
    }

    private HojumoneyRecommendationResponse.RecommendedStockResponse toRecommendedStockResponse(
            HojumoneyRecommendationCandidate candidate,
            String sortMetricName
    ) {
        Stock stock = candidate.getStock();
        return new HojumoneyRecommendationResponse.RecommendedStockResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName(),
                0,
                candidate.getTags().stream().toList(),
                candidate.matchedConditionCount(),
                sortMetricName,
                candidate.getSortMetricValue()
        );
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
                    item.sortMetricName(),
                    item.sortMetricValue()
            ));
        }
        return ranked;
    }
}
