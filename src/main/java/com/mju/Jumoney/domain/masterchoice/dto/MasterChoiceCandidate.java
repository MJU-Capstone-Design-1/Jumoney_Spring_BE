package com.mju.Jumoney.domain.masterchoice.dto;

import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockIndicator;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public class MasterChoiceCandidate {

    private final Stock stock;
    private final StockIndicator indicator;
    private final Set<MasterOptionLogicCode> matchedOptions = new LinkedHashSet<>();
    private BigDecimal sortMetricValue;
    private BigDecimal fallbackSortMetricValue;

    public MasterChoiceCandidate(StockIndicator indicator) {
        this.indicator = indicator;
        this.stock = indicator.getStock();
    }

    public Stock getStock() {
        return stock;
    }

    public StockIndicator getIndicator() {
        return indicator;
    }

    public Set<MasterOptionLogicCode> getMatchedOptions() {
        return matchedOptions;
    }

    public BigDecimal getSortMetricValue() {
        return sortMetricValue;
    }

    public void setSortMetricValue(BigDecimal sortMetricValue) {
        this.sortMetricValue = sortMetricValue;
    }

    public BigDecimal getFallbackSortMetricValue() {
        return fallbackSortMetricValue;
    }

    public void setFallbackSortMetricValue(BigDecimal fallbackSortMetricValue) {
        this.fallbackSortMetricValue = fallbackSortMetricValue;
    }

    public void addMatchedOption(MasterOptionLogicCode logicCode) {
        this.matchedOptions.add(logicCode);
    }

    public int matchedConditionCount() {
        return matchedOptions.size();
    }
}
