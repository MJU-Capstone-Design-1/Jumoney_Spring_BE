package com.mju.Jumoney.global.client.kis.dto.dividend;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mju.Jumoney.global.client.kis.dto.common.KisApiResponse;

import java.util.List;

// 배당일정 API는 상세 목록을 output1 배열로 내려줍니다.
public record KisDividendResponse(
        @JsonProperty("rt_cd") String resultCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output1") List<KisDividendOutput> output
) implements KisApiResponse {
}
