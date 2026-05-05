package com.mju.Jumoney.global.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// KIS 공통 응답 필드와 현재가 시세 output을 함께 받는 DTO입니다.
public record KisCurrentPriceResponse(
        @JsonProperty("rt_cd") String resultCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output") KisCurrentPriceOutput output
) implements KisApiResponse {
}
