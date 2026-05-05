package com.mju.Jumoney.global.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// FHKST66430200(국내주식 손익계산서)의 output 한 행입니다.
public record KisIncomeStatementOutput(
        @JsonProperty("stac_yymm") String settlementYearMonth,
        @JsonProperty("sale_account") String sales,
        @JsonProperty("bsop_prti") String operatingProfit
) {
}
