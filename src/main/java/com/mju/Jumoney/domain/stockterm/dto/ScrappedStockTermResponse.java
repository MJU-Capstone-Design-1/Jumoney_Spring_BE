package com.mju.Jumoney.domain.stockterm.dto;

public record ScrappedStockTermResponse(
        Long termId,
        String categoryName,
        String termName,
        boolean isLearned
) {
}
