package com.mju.Jumoney.global.client.kis.dto.price;

import com.fasterxml.jackson.annotation.JsonProperty;

// FHKST01010300(주식현재가 체결)의 output 한 행입니다.
public record KisExecutionStrengthOutput(
        @JsonProperty("stck_cntg_hour") String executionTime,
        @JsonProperty("stck_prpr") String currentPrice,
        @JsonProperty("cntg_vol") String executionVolume,
        @JsonProperty("tday_rltv") String executionStrength
) {
}
