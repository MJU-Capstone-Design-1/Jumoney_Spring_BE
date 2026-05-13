package com.mju.Jumoney.global.realtime;

import java.math.BigDecimal;

public record StockRealtimeSnapshot(
        String code,
        String time,
        BigDecimal price,
        BigDecimal change,
        BigDecimal rate,
        Long vol,
        BigDecimal strength,
        Long timestamp
) {
}
