package com.mju.Jumoney.domain.masterchoice.dto;

import java.time.LocalDate;

public record MasterChoiceBacktestDataStatusResponse(
        String stockCode,
        String stockName,
        long financialCount,
        String latestSettlementYearMonth,
        LocalDate latestFinancialAvailableDate,
        long dailyIndicatorCount,
        LocalDate latestDailyIndicatorTradeDate
) {
}
