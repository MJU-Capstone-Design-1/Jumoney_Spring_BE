package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.enums.HojumoneyRecommendationTag;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

public class HojumoneyRecommendationCandidate {

    private final Stock stock;
    private StockIndicator indicator;
    private final EnumSet<HojumoneyRecommendationTag> tags = EnumSet.noneOf(HojumoneyRecommendationTag.class);
    private BigDecimal sortMetricValue;

    public HojumoneyRecommendationCandidate(Stock stock) {
        this.stock = stock;
    }

    public Stock getStock() {
        return stock;
    }

    public StockIndicator getIndicator() {
        return indicator;
    }

    public void setIndicator(StockIndicator indicator) {
        this.indicator = indicator;
    }

    public Set<HojumoneyRecommendationTag> getTags() {
        return tags;
    }

    public void addTag(HojumoneyRecommendationTag tag) {
        this.tags.add(tag);
    }

    public int matchedConditionCount() {
        return tags.size();
    }

    public BigDecimal getSortMetricValue() {
        return sortMetricValue;
    }

    public void setSortMetricValue(BigDecimal sortMetricValue) {
        this.sortMetricValue = sortMetricValue;
    }
}
