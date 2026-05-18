package com.mju.Jumoney.domain.hojumoney.dto;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;

import java.math.BigDecimal;
import java.util.List;

public record HojumoneyRecommendationResponse(
        Long recommendationId,
        SurveyLogicCode investmentPurpose,
        SurveyLogicCode riskProfile,
        SurveyLogicCode investmentHorizon,
        HojumoneyPersonaResponse persona,
        int totalCount,
        List<RecommendedStockResponse> recommendations
) {

    public HojumoneyRecommendationResponse withRecommendationId(Long recommendationId) {
        return new HojumoneyRecommendationResponse(
                recommendationId,
                investmentPurpose,
                riskProfile,
                investmentHorizon,
                persona,
                totalCount,
                recommendations
        );
    }

    public record HojumoneyPersonaResponse(
            String personaName,
            String personaDescription
    ) {
    }

    public record RecommendedStockResponse(
            Long stockId,
            String stockCode,
            String stockName,
            int rank,
            List<SurveyLogicCode> tags,
            List<String> goodSectorTags,
            int matchedConditionCount,
            String sortMetricKey,
            BigDecimal sortMetricValue,
            BigDecimal currentPrice,
            BigDecimal changeRate
    ) {
    }
}
