package com.mju.Jumoney.global.client.kis.dto.trading;

import com.mju.Jumoney.global.client.kis.dto.common.KisApiResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// 투자자매매동향 API는 요약 output1과 일별 목록 output2를 함께 주며, 추천에는 output2를 사용합니다.
public record KisInvestorTradeDailyResponse(
        @JsonProperty("rt_cd") String resultCode,
        @JsonProperty("msg_cd") String messageCode,
        @JsonProperty("msg1") String message,
        @JsonProperty("output2") List<KisInvestorTradeDailyOutput> output
) implements KisApiResponse {
}
