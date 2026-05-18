package com.mju.Jumoney.global.client.kis.dto.condition;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisHtsConditionResultOutput(
        @JsonProperty("code") String stockCode,
        @JsonProperty("name") String stockName,
        @JsonProperty("price") String currentPrice,
        @JsonProperty("chgrate") String changeRate,
        @JsonProperty("trade_amt") String tradeAmount,
        @JsonProperty("cttr") String executionStrength,
        @JsonProperty("stotprice") String marketCap
) {
}
