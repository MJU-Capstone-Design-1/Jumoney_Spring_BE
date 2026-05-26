package com.mju.Jumoney.global.realtime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
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
        Long tradeAmount,
        @JsonDeserialize(using = RealtimeBigDecimalDeserializer.class)
        BigDecimal strength
) {
}
