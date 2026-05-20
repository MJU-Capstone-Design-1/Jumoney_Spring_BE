package com.mju.Jumoney.domain.mockinvestment.dto;

import java.util.List;

public record MockInvestmentOrderHistoryResponse(
        List<MockInvestmentOrderHistoryItemResponse> orders
) {
}
