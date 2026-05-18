package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

import java.math.BigDecimal;
import java.util.List;

public record MasterPortfolioDescriptionResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName,
        String basePeriod,
        RepresentativeCaseResponse representativeCase,
        List<StockResponse> stocks
) {

    public record RepresentativeCaseResponse(
            String stockName,
            String sector,
            String investmentPeriod,
            String investmentResult,
            String title,
            String description
    ) {
    }

    public record StockResponse(
            String stockName,
            String sector,
            BigDecimal weight
    ) {
    }
}
