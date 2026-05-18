package com.mju.Jumoney.domain.stockterm.dto;

import com.mju.Jumoney.domain.stockterm.enums.StockTermCategory;

public record StockTermInitDto(
        StockTermCategory category,
        String termName,
        String subtitle,
        String description,
        String imageFileName
) {
}
