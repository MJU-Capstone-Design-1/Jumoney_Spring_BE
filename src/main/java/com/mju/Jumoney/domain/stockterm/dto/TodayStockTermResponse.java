package com.mju.Jumoney.domain.stockterm.dto;

public record TodayStockTermResponse(
        Long termId,
        String termName,
        String description
) {
}
