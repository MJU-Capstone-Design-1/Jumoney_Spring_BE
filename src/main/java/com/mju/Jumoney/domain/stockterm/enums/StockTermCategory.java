package com.mju.Jumoney.domain.stockterm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum StockTermCategory {
    BASIC_CONCEPT("기초 개념"),
    COMPANY_DIAGNOSIS("기업 진단"),
    CHART_ANALYSIS("차트 분석"),
    TRADING_PRACTICE("거래 실무");

    private final String label;

    public int getCategoryId() {
        return ordinal() + 1;
    }

    public static StockTermCategory fromCategoryId(int categoryId) {
        return Arrays.stream(values())
                .filter(category -> category.getCategoryId() == categoryId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 카테고리 ID입니다: " + categoryId));
    }
}
