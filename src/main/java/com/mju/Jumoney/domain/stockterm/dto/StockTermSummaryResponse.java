package com.mju.Jumoney.domain.stockterm.dto;

public record StockTermSummaryResponse(
        Long termId,
        String termName,
        boolean isScrapped,
        boolean isLearned
) {
}
