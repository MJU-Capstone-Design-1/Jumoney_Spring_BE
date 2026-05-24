package com.mju.Jumoney.global.realtime;

import java.math.BigDecimal;

public record StockRealtimeSnapshot(
        String code,
        Long minuteTs,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal change,
        BigDecimal rate,
        Long volume,
        BigDecimal strength
) {
}
