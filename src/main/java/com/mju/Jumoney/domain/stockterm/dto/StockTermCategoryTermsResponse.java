package com.mju.Jumoney.domain.stockterm.dto;

import java.util.List;

public record StockTermCategoryTermsResponse(
        int categoryId,
        String categoryName,
        List<StockTermSummaryResponse> terms
) {
}
