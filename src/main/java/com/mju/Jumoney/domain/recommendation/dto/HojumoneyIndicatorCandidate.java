package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;

public record HojumoneyIndicatorCandidate(
        Stock stock,
        StockIndicator indicator
) {
    public static HojumoneyIndicatorCandidate from(StockIndicator indicator) {
        return new HojumoneyIndicatorCandidate(indicator.getStock(), indicator);
    }
}
