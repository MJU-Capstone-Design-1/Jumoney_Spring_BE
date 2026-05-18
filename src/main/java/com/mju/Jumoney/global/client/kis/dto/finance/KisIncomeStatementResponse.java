package com.mju.Jumoney.global.client.kis.dto.finance;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mju.Jumoney.global.client.kis.dto.common.KisApiResponse;

import java.util.List;

// KIS 공통 응답 필드와 손익계산서 output 배열을 함께 받는 DTO입니다.
public record KisIncomeStatementResponse(
        @JsonProperty("rt_cd") String resultCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output") List<KisIncomeStatementOutput> output
) implements KisApiResponse {
}
