package com.mju.Jumoney.global.client.kis.smoke.dto;

import com.mju.Jumoney.domain.stock.enums.HtsSearchType;

import java.time.LocalDate;
import java.util.Map;

public record HtsConditionBatchRunResponse(
        LocalDate baseDate,
        int totalSavedCount,
        Map<HtsSearchType, Integer> savedCounts
) {
    public static HtsConditionBatchRunResponse of(LocalDate baseDate, Map<HtsSearchType, Integer> savedCounts) {
        int totalSavedCount = savedCounts.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        return new HtsConditionBatchRunResponse(baseDate, totalSavedCount, savedCounts);
    }
}
