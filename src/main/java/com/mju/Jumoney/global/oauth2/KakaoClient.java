package com.mju.Jumoney.global.oauth2;

import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class KakaoClient {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret}")
    private String clientSecret;

    @Value("${kakao.allowed-redirect-uris}")
    private String allowedRedirectUris;

    @Value("${kakao.token-uri}")
    private String tokenUri;

    @Value("${kakao.user-info-uri}")
    private String userInfoUri;

    private final WebClient webClient = WebClient.create();

    // 인가 코드로 카카오 Access Token 요청하기
    public String getAccessToken(String code, String redirectUri) {
        validateRedirectUri(redirectUri);
        log.info("[KakaoClient] Access Token 요청 시작 - redirectUri={}", redirectUri);

        Map response = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", clientId)
                        .with("redirect_uri", redirectUri)
                        .with("code", code)
                        .with("client_secret", clientSecret))
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(errorBody -> {
                                    log.error(
                                            "[KakaoClient] 카카오 Access Token 발급 실패 - status={}, redirectUri={}, response={}",
                                            clientResponse.statusCode(),
                                            redirectUri,
                                            errorBody
                                    );
                                    return Mono.error(new RuntimeException("카카오 인증 실패: Access Token 발급 오류"));
                                })
                )
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("access_token")) {
            log.error("[KakaoClient] 카카오 Access Token 발급 실패: 응답이 올바르지 않습니다.");
            throw new RuntimeException("카카오 인증 실패: Access Token 발급 오류");
        }

        log.info("[KakaoClient] 카카오 Access Token 발급 완료");
        return response.get("access_token").toString();
    }

    private void validateRedirectUri(String redirectUri) {
        if (!StringUtils.hasText(redirectUri) || !getAllowedRedirectUriList().contains(redirectUri)) {
            log.warn("[KakaoClient] 허용되지 않은 카카오 redirectUri 요청 - redirectUri={}", redirectUri);
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private List<String> getAllowedRedirectUriList() {
        return Arrays.stream(allowedRedirectUris.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    // 카카오 Access Token으로 카카오 사용자 정보(ID, 닉네임) 가져오기
    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        log.info("[KakaoClient] 사용자 정보 요청 시작");

        Map response = webClient.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .build()
                .get()
                .uri(userInfoUri)
                .retrieve()
                .bodyToMono(Map.class)
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
