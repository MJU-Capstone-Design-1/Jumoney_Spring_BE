package com.mju.Jumoney.domain.recommendation.service;

import com.mju.Jumoney.domain.recommendation.domain.HojumoneyRecommendation;
import com.mju.Jumoney.domain.recommendation.domain.Recommendation;
import com.mju.Jumoney.domain.recommendation.domain.RecommendationStock;
import com.mju.Jumoney.domain.recommendation.domain.RecommendationStockTag;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationResponse;
import com.mju.Jumoney.domain.recommendation.enums.RecommendationStockTagType;
import com.mju.Jumoney.domain.recommendation.enums.RecommendationType;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.recommendation.repository.HojumoneyRecommendationRepository;
import com.mju.Jumoney.domain.recommendation.repository.RecommendationRepository;
import com.mju.Jumoney.domain.recommendation.repository.RecommendationStockRepository;
import com.mju.Jumoney.domain.recommendation.repository.RecommendationStockTagRepository;
import com.mju.Jumoney.domain.sector.enums.SectorType;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HojumoneyRecommendationQueryService {

    private final RecommendationRepository recommendationRepository;
    private final HojumoneyRecommendationRepository hojumoneyRecommendationRepository;
    private final RecommendationStockRepository recommendationStockRepository;
    private final RecommendationStockTagRepository recommendationStockTagRepository;

    @Transactional(readOnly = true)
    public HojumoneyRecommendationResponse getLatestRecommendation(Long userId) {
        Recommendation recommendation = recommendationRepository
                .findTopByUserIdAndRecommendationTypeOrderByCreatedAtDesc(userId, RecommendationType.HOJUMONEY)
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.HOJUMONEY_RECOMMENDATION_NOT_FOUND));
        return toResponse(recommendation);
    }

    private HojumoneyRecommendationResponse toResponse(Recommendation recommendation) {
        HojumoneyRecommendation hojummoneyRecommendation = hojumoneyRecommendationRepository.findByRecommendationId(recommendation.getId())
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.HOJUMONEY_RECOMMENDATION_NOT_FOUND));

        List<RecommendationStock> recommendationStocks = recommendationStockRepository
                .findByRecommendationIdOrderByRankAsc(recommendation.getId());
        if (recommendationStocks.isEmpty()) {
            return new HojumoneyRecommendationResponse(
                    recommendation.getId(),
                    hojummoneyRecommendation.getInvestmentPurpose(),
                    hojummoneyRecommendation.getRiskProfile(),
                    hojummoneyRecommendation.getInvestmentHorizon(),
                    new HojumoneyRecommendationResponse.HojumoneyPersonaResponse(
                            hojummoneyRecommendation.getPersonaName(),
                            hojummoneyRecommendation.getPersonaDescription()
                    ),
                    0,
                    List.of()
            );
        }
        Map<Long, List<RecommendationStockTag>> tagsByRecommendationStockId = recommendationStockTagRepository
                .findByRecommendationStockIdIn(recommendationStocks.stream().map(RecommendationStock::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(RecommendationStockTag::getRecommendationStockId));

        List<HojumoneyRecommendationResponse.RecommendedStockResponse> responses = recommendationStocks.stream()
                .map(stock -> toRecommendedStockResponse(stock, tagsByRecommendationStockId.getOrDefault(stock.getId(), List.of())))
                .toList();

        return new HojumoneyRecommendationResponse(
                recommendation.getId(),
                hojummoneyRecommendation.getInvestmentPurpose(),
                hojummoneyRecommendation.getRiskProfile(),
                hojummoneyRecommendation.getInvestmentHorizon(),
                new HojumoneyRecommendationResponse.HojumoneyPersonaResponse(
                        hojummoneyRecommendation.getPersonaName(),
                        hojummoneyRecommendation.getPersonaDescription()
                ),
                responses.size(),
                responses
        );
    }

    private HojumoneyRecommendationResponse.RecommendedStockResponse toRecommendedStockResponse(
            RecommendationStock recommendationStock,
            Collection<RecommendationStockTag> tags
    ) {
        List<SurveyLogicCode> surveyLogicTags = tags.stream()
                .filter(tag -> tag.getTagType() == RecommendationStockTagType.SURVEY_LOGIC)
                .map(RecommendationStockTag::getTagName)
                .map(this::toSurveyLogicCode)
                .toList();
        List<String> goodSectorTags = tags.stream()
                .filter(tag -> tag.getTagType() == RecommendationStockTagType.GOOD_SECTOR)
                .map(RecommendationStockTag::getTagName)
                .map(this::toSectorTypeName)
                .toList();

        return new HojumoneyRecommendationResponse.RecommendedStockResponse(
                recommendationStock.getStock().getId(),
                recommendationStock.getStock().getStockCode(),
                recommendationStock.getStock().getName(),
                recommendationStock.getRank(),
                surveyLogicTags,
                goodSectorTags,
                recommendationStock.getMatchedConditionCount(),
                recommendationStock.getSortMetricKey(),
                recommendationStock.getSortMetricValue(),
                recommendationStock.getCurrentPrice(),
                recommendationStock.getChangeRate()
        );
    }

    private SurveyLogicCode toSurveyLogicCode(String tagName) {
        try {
            return SurveyLogicCode.valueOf(tagName);
        } catch (IllegalArgumentException ignored) {
        }
        return Arrays.stream(SurveyLogicCode.values())
                .filter(code -> code.getLabel().equals(tagName))
                .findFirst()
                .orElseThrow(() -> new CustomException(RecommendationErrorCode.HOJUMONEY_RECOMMENDATION_NOT_FOUND));
    }

    private String toSectorTypeName(String tagName) {
        try {
            return SectorType.valueOf(tagName).name();
        } catch (IllegalArgumentException ignored) {
        }
        return SectorType.fromDescription(tagName).name();
    }
}
