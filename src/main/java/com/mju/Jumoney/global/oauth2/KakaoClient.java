package com.mju.Jumoney.global.oauth2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Slf4j
public class KakaoClient {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    private final WebClient webClient = WebClient.create();

    // 인가 코드로 카카오 Access Token 요청하기
    public String getAccessToken(String code) {
        log.info("[KakaoClient] Access Token 요청: code={}", code);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);
        formData.add("client_secret", clientSecret);

        JsonNode response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("access_token")) {
            log.error("[KakaoClient] 카카오 Access Token 발급 실패: 응답이 올바르지 않습니다.");
            throw new RuntimeException("카카오 인증 실패: Access Token 발급 오류");
        }

        log.info("[KakaoClient] 카카오 Access Token 발급 완료");
        return response.get("access_token").asText();
    }

    // 카카오 Access Token으로 카카오 사용자 정보(ID, 닉네임) 가져오기
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        log.info("[KakaoClient] 사용자 정보 요청 시작");

        JsonNode response = webClient.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build()
                .get()
                .uri(userInfoUri)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) {
            log.error("[KakaoClient] 카카오 사용자 정보 조회 실패");
            throw new RuntimeException("카카오 인증 실패: 사용자 정보 응답 없음");
        }

        KakaoUserInfoResponse userInfo = new KakaoUserInfoResponse(response);
        log.info("[KakaoClient] 사용자 정보 조회 성공 - 카카오 고유 ID: {}", userInfo.getId());

        return userInfo;
    }
}
