package com.mju.Jumoney.domain.masterchoice.dto;

import java.time.LocalDate;
import java.util.List;

public record MasterChoiceBacktestDataSyncResponse(
        String syncType,
        LocalDate fromDate,
        LocalDate toDate,
        int totalCount,
        int successCount,
        int failureCount,
        List<Item> items
) {

    public record Item(
            String stockCode,
            String stockName,
            boolean success,
            int savedCount,
            String message
    ) {
        public static Item success(String stockCode, String stockName, int savedCount) {
            return new Item(stockCode, stockName, true, savedCount, null);
        }

        public static Item failure(String stockCode, String stockName, String message) {
            return new Item(stockCode, stockName, false, 0, message);
        }
    }
}
