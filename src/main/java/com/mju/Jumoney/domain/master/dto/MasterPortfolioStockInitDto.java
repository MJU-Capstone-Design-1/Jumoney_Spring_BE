package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

import java.math.BigDecimal;

public record MasterPortfolioStockInitDto(
        MasterCode masterCode,
        String stockName,
        String sector,
        BigDecimal weight
) {
}
