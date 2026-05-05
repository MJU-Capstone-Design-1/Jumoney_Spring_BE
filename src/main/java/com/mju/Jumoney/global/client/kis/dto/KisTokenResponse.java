package com.mju.Jumoney.global.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// KIS 한국투자증권 토큰 발급 API 응답 DTO (받아온 JSON 데이터를 자바 객체로)
public record KisTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("access_token_token_expired") String tokenExpired,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn
) {
}
