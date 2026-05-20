package com.mju.Jumoney.domain.mockinvestment.dto;

import java.util.List;

public record MockInvestmentSectorStocksResponse(
        Long sectorId,
        String sectorName,
        List<MockInvestmentSectorStockItemResponse> stocks
) {
}
