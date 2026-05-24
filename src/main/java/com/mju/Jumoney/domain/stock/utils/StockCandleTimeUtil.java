package com.mju.Jumoney.domain.stock.utils;

import java.time.LocalDateTime;

public final class StockCandleTimeUtil {

    private StockCandleTimeUtil() {
    }

    public static LocalDateTime toThirtyMinuteBucketStart(LocalDateTime candleTime) {
        int flooredMinute = candleTime.getMinute() >= 30 ? 30 : 0;
        return candleTime.withMinute(flooredMinute).withSecond(0).withNano(0);
    }
}
