package com.mju.Jumoney.global.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// KIS 공통 응답 필드와 신용잔고 output 배열을 함께 받는 DTO입니다.
public record KisCreditBalanceResponse(
        @JsonProperty("rt_cd") String resultCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output") List<KisCreditBalanceOutput> output
) implements KisApiResponse {
}
