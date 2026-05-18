package com.mju.Jumoney.domain.stockterm.dto;

public record StockTermScrapToggleResponse(
        Long termId,
        boolean isScrapped
) {
}
