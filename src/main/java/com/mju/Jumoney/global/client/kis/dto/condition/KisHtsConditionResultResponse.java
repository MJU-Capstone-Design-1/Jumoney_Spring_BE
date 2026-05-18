package com.mju.Jumoney.global.client.kis.dto.condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mju.Jumoney.global.client.kis.dto.common.KisApiResponse;

import java.util.List;

public record KisHtsConditionResultResponse(
        @JsonProperty("rt_cd") String resultCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output2") List<KisHtsConditionResultOutput> output
) implements KisApiResponse {
}
