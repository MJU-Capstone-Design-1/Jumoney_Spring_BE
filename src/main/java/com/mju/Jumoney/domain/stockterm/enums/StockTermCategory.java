package com.mju.Jumoney.domain.stockterm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockTermCategory {
    BASIC_CONCEPT("기초 개념"),
    COMPANY_DIAGNOSIS("기업 진단"),
    CHART_ANALYSIS("차트 분석"),
    TRADING_PRACTICE("거래 실무");

    private final String label;
}
