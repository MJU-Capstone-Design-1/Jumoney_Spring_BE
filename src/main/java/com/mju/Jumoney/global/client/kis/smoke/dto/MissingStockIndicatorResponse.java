package com.mju.Jumoney.global.client.kis.smoke.dto;

import com.mju.Jumoney.domain.stock.domain.Stock;

public record MissingStockIndicatorResponse(
        Long stockId,
        String stockCode,
        String stockName
) {
    public static MissingStockIndicatorResponse from(Stock stock) {
        return new MissingStockIndicatorResponse(
                stock.getId(),
                stock.getStockCode(),
                stock.getName()
        );
    }
}
