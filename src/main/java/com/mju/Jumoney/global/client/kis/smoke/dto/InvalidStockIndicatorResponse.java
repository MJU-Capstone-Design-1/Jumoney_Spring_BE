package com.mju.Jumoney.global.client.kis.smoke.dto;

import com.mju.Jumoney.domain.stock.domain.StockIndicator;

import java.util.ArrayList;
import java.util.List;

public record InvalidStockIndicatorResponse(
        Long stockId,
        String stockCode,
        String stockName,
        List<String> nullFields
) {

    public static InvalidStockIndicatorResponse from(StockIndicator indicator) {
        List<String> nullFields = new ArrayList<>();
        addIfNull(nullFields, "marketCap", indicator.getMarketCap());
        addIfNull(nullFields, "accumulatedTradeAmount", indicator.getAccumulatedTradeAmount());
        addIfNull(nullFields, "executionStrength", indicator.getExecutionStrength());
        addIfNull(nullFields, "debtRatio", indicator.getDebtRatio());
        addIfNull(nullFields, "operatingProfit", indicator.getOperatingProfit());
        addIfNull(nullFields, "operatingProfitGrowthRate", indicator.getOperatingProfitGrowthRate());
        addIfNull(nullFields, "dps", indicator.getDps());
        addIfNull(nullFields, "dividendYield", indicator.getDividendYield());
        addIfNull(nullFields, "roe", indicator.getRoe());
        addIfNull(nullFields, "per", indicator.getPer());
        addIfNull(nullFields, "pbr", indicator.getPbr());
        addIfNull(nullFields, "currentEps", indicator.getCurrentEps());
        addIfNull(nullFields, "currentSales", indicator.getCurrentSales());
        addIfNull(nullFields, "marginDebtRate", indicator.getMarginDebtRate());
        addIfNull(nullFields, "high52WeekRate", indicator.getHigh52WeekRate());
        addIfNull(nullFields, "instNetBuy20Days", indicator.getInstNetBuy20Days());

        return new InvalidStockIndicatorResponse(
                indicator.getStock().getId(),
                indicator.getStock().getStockCode(),
                indicator.getStock().getName(),
                nullFields
        );
    }

    private static void addIfNull(List<String> nullFields, String fieldName, Object value) {
        if (value == null) {
            nullFields.add(fieldName);
        }
    }
}
