package com.mju.Jumoney.global.client.kis.dto.dividend;

import com.fasterxml.jackson.annotation.JsonProperty;

// HHKDB669102C0(예탁원정보 배당일정)의 output1 한 행입니다.
public record KisDividendOutput(
        @JsonProperty("record_date") String recordDate,
        @JsonProperty("sht_cd") String stockCode,
        @JsonProperty("isin_name") String stockName,
        @JsonProperty("divi_kind") String dividendKind,
        @JsonProperty("per_sto_divi_amt") String cashDividendPerShare
) {
}
