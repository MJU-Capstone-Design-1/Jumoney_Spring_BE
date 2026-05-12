package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.stock.domain.HtsStock;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.enums.HtsSearchType;

import java.time.LocalDate;

public record HojumoneyRiskCandidate(
        Stock stock,
        HtsSearchType searchType,
        LocalDate baseDate
) {
    public static HojumoneyRiskCandidate from(HtsStock htsStock) {
        return new HojumoneyRiskCandidate(
                htsStock.getStock(),
                htsStock.getSearchType(),
                htsStock.getBaseDate()
        );
    }
}
