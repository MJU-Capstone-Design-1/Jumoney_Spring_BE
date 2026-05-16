package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;

import java.math.BigDecimal;
import java.util.List;

public record MasterRecommendationResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName,
        List<Long> selectedOptionIds,
        int totalCount,
        List<RecommendedStockResponse> recommendations
) {

    public record RecommendedStockResponse(
            Long stockId,
            String stockCode,
            String stockName,
            int rank,
            List<MasterOptionLogicCode> tags,
            List<String> goodSectorTags,
            int matchedConditionCount,
            String sortMetricKey,
            BigDecimal sortMetricValue,
            BigDecimal currentPrice,
            BigDecimal changeRate
    ) {
    }
}
