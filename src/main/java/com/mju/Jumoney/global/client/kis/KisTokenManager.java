package com.mju.Jumoney.global.client.kis;

import com.mju.Jumoney.global.client.kis.dto.KisTokenResponse;
import com.mju.Jumoney.global.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

// KIS API 인증 토큰 관리 클래스
// 한국투자증권 API 호출에 필요한 Access Token을 발급받고 Redis에 캐싱하여 관리합니다.
@Component
@Slf4j
public class KisTokenManager {

    private final WebClient webClient;
    private final RedisUtil redisUtil;

    @Value("${kis.appkey}")
    private String appKey;

    @Value("${kis.appsecret}")
    private String appSecret;

    private static final String KIS_TOKEN_REDIS_KEY = "kis:token";
    private static final Duration KIS_TOKEN_TTL = Duration.ofHours(23);
    private static final Duration REDIS_CIRCUIT_OPEN_DURATION = Duration.ofMinutes(5);
    private final Object tokenLock = new Object();
    private final AtomicLong redisCircuitOpenUntilMillis = new AtomicLong(0);
    private volatile String localCachedToken;
    private volatile Instant localTokenExpiresAt;

    public KisTokenManager(@Qualifier("kisWebClient") WebClient webClient, RedisUtil redisUtil) {
        this.webClient = webClient;
        this.redisUtil = redisUtil;
    }

    public String getAccessToken() {
        String cachedToken = getCachedToken();
        if (cachedToken != null) {
            return cachedToken;
        }

        synchronized (tokenLock) {
            cachedToken = getCachedToken();
            if (cachedToken != null) {
                return cachedToken;
            }

            log.info("[KIS] Access Token 만료 또는 캐시 없음. 신규 발급 요청");
            String newToken = issueNewToken();
            saveToken(newToken);
            cacheTokenLocally(newToken);

            return newToken;
        }
    }

    private String getCachedToken() {
        if (isRedisCircuitOpen()) {
            return getLocalCachedToken();
        }

        try {
            String token = redisUtil.get(KIS_TOKEN_REDIS_KEY, String.class).orElse(null);
            closeRedisCircuit();
            return token;
        } catch (Exception e) {
            openRedisCircuit();
            log.warn("[KIS] Redis 연결 장애 발생. 로컬 캐시를 확인합니다. Error: {}", e.getMessage());
            return getLocalCachedToken();
        }
    }

    private void saveToken(String token) {
        if (isRedisCircuitOpen()) {
            return;
        }

        try {
            redisUtil.save(KIS_TOKEN_REDIS_KEY, token, KIS_TOKEN_TTL);
            closeRedisCircuit();
        } catch (Exception e) {
            openRedisCircuit();
            log.warn("[KIS] 신규 발급된 토큰을 Redis에 저장하는 데 실패했습니다. 로컬 캐시로 대체합니다. Error: {}", e.getMessage());
        }
    }

    private String getLocalCachedToken() {
        if (localCachedToken == null || localTokenExpiresAt == null) {
            return null;
        }

        if (Instant.now().isAfter(localTokenExpiresAt)) {
            localCachedToken = null;
            localTokenExpiresAt = null;
            return null;
        }

        return localCachedToken;
    }

    private void cacheTokenLocally(String token) {
        localCachedToken = token;
        localTokenExpiresAt = Instant.now().plus(KIS_TOKEN_TTL);
    }

    private boolean isRedisCircuitOpen() {
        return System.currentTimeMillis() < redisCircuitOpenUntilMillis.get();
    }

    private void openRedisCircuit() {
        redisCircuitOpenUntilMillis.set(System.currentTimeMillis() + REDIS_CIRCUIT_OPEN_DURATION.toMillis());
    }

    private void closeRedisCircuit() {
        redisCircuitOpenUntilMillis.set(0);
    }

    private String issueNewToken() {
        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        KisTokenResponse response = webClient.post()
                .uri("/oauth2/tokenP")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class).map(body -> {
                            log.error("[KIS] 토큰 발급 클라이언트 에러: {}", body);
                            return new RuntimeException("한국투자증권 API 클라이언트 에러: " + body);
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class).map(body -> {
                            log.error("[KIS] 토큰 발급 서버 에러: {}", body);
                            return new RuntimeException("한국투자증권 API 서버 에러: " + body);
                        })
                )
                .bodyToMono(KisTokenResponse.class)
                .block();

        if (response == null || response.accessToken() == null) {
            log.error("[KIS] 한국투자증권 토큰 발급 실패 (응답이 비어있음)");
            throw new RuntimeException("한국투자증권 API 토큰 발급 실패");
        }

        log.info("[KIS] Access Token 신규 발급 완료");
        return response.accessToken();
    }
}
