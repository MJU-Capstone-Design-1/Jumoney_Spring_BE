package com.mju.Jumoney.global.realtime;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record RealtimeMinuteCandle(
        String stockCode,
        LocalDateTime candleTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume,
        Long tradeAmount,
        boolean isFinal
) {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    public static RealtimeMinuteCandle from(StockRealtimeSnapshot snapshot) {
        return new RealtimeMinuteCandle(
                snapshot.code(),
                toKstLocalDateTime(snapshot.minuteTs()),
                snapshot.open(),
                snapshot.high(),
                snapshot.low(),
                snapshot.close(),
                snapshot.volume(),
                null,
                false
        );
    }

    private static LocalDateTime toKstLocalDateTime(Long minuteTs) {
        if (minuteTs == null) {
            return null;
        }
        return Instant.ofEpochMilli(minuteTs)
                .atZone(KST_ZONE_ID)
                .toLocalDateTime();
    }
}
