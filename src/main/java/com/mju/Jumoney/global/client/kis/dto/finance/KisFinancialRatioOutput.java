package com.mju.Jumoney.global.client.kis.dto.finance;

import com.fasterxml.jackson.annotation.JsonProperty;

// FHKST66430300(국내주식 재무비율)의 output 한 행입니다. 결산년월로 올해/전년 데이터를 구분합니다.
public record KisFinancialRatioOutput(
        @JsonProperty("stac_yymm") String settlementYearMonth,
        @JsonProperty("grs") String salesGrowthRate,
        @JsonProperty("bsop_prfi_inrt") String operatingProfitGrowthRate,
        @JsonProperty("roe_val") String roe,
        @JsonProperty("eps") String eps,
        @JsonProperty("lblt_rate") String debtRatio
) {
}
