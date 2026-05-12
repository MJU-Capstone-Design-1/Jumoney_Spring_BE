package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.enums.HojumoneyRecommendationTag;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;

import java.math.BigDecimal;
import java.util.List;

public record HojumoneyRecommendationResponse(
        SurveyLogicCode investmentPurpose,
        SurveyLogicCode riskProfile,
        SurveyLogicCode investmentHorizon,
        int totalCount,
        List<RecommendedStockResponse> recommendations
) {

    public record RecommendedStockResponse(
            Long stockId,
            String stockCode,
            String stockName,
            int rank,
            List<HojumoneyRecommendationTag> tags,
            int matchedConditionCount,
            String sortMetricName,
            BigDecimal sortMetricValue
    ) {
    }
}
