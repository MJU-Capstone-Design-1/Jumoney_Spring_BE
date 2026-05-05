package com.mju.Jumoney.global.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// FHKST01010100(주식현재가 시세)의 output 중 추천 지표에 필요한 필드만 매핑합니다.
public record KisCurrentPriceOutput(
        @JsonProperty("stck_prpr") String currentPrice,
        @JsonProperty("hts_avls") String marketCap,
        @JsonProperty("per") String per,
        @JsonProperty("pbr") String pbr,
        @JsonProperty("acml_tr_pbmn") String accumulatedTradeAmount,
        @JsonProperty("d250_hgpr_vrss_prpr_rate") String twoHundredFiftyDayHighPriceRate,
        @JsonProperty("w52_hgpr_vrss_prpr_ctrt") String fiftyTwoWeekHighPriceRate
) {
}
