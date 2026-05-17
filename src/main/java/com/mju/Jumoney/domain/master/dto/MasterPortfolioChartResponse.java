package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

import java.math.BigDecimal;
import java.util.List;

public record MasterPortfolioChartResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName,
        String basePeriod,
        List<SectorChartResponse> sectorChart,
        List<CompanyRatioChartResponse> companyRatioChart
) {

    public record SectorChartResponse(
            String sector,
            BigDecimal weight
    ) {
    }

    public record CompanyRatioChartResponse(
            String stockName,
            BigDecimal weight
    ) {
    }
}
