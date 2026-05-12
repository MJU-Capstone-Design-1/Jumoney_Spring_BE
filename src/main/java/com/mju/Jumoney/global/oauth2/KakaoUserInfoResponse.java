package com.mju.Jumoney.global.oauth2;

import lombok.Getter;

import java.util.Map;

@Getter
// 카카오에서 받아온 사용자 정보(JSON)를 파싱하는 DTO
public class KakaoUserInfoResponse {
    private final Long id;
    private final String nickname;

    public KakaoUserInfoResponse(Map attributes) {
        this.id = Long.valueOf(attributes.get("id").toString());
        Map properties = (Map) attributes.get("properties");
        this.nickname = properties.get("nickname").toString();
    }
}
