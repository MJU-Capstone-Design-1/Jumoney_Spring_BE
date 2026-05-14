package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;

import java.math.BigDecimal;
import java.util.List;

public record HojumoneyRecommendationResponse(
        SurveyLogicCode investmentPurpose,
        SurveyLogicCode riskProfile,
        SurveyLogicCode investmentHorizon,
        HojumoneyPersonaResponse persona,
        int totalCount,
        List<RecommendedStockResponse> recommendations
) {

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
            int matchedConditionCount,
            String sortMetricKey,
            BigDecimal sortMetricValue,
            BigDecimal currentPrice,
            BigDecimal changeRate
    ) {
    }
}
