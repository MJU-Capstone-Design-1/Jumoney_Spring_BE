package com.mju.Jumoney.domain.stockterm.dto;

public record StockTermDetailResponse(
        Long termId,
        int categoryId,
        String categoryName,
        String termName,
        String description,
        boolean isScrapped,
        boolean isLearned
) {
}
