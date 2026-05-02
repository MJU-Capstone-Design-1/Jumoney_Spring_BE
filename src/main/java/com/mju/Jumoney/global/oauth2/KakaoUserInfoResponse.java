package com.mju.Jumoney.global.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
// 카카오에서 받아온 사용자 정보(JSON)를 파싱하는 DTO
public class KakaoUserInfoResponse {
    private final Long id;
    private final String nickname;

    public KakaoUserInfoResponse(JsonNode jsonNode) {
        this.id = jsonNode.get("id").asLong();
        this.nickname = jsonNode.get("properties").get("nickname").asText();
    }
}
