package com.mju.Jumoney.domain.mockinvestment.dto;

import java.util.List;

public record MockInvestmentStockSearchResponse(
        String keyword,
        List<MockInvestmentSectorStockItemResponse> stocks
) {
}
