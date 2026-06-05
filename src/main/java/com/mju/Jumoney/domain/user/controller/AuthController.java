package com.mju.Jumoney.domain.user.controller;

import com.mju.Jumoney.domain.user.dto.AuthLoginResponse;
import com.mju.Jumoney.domain.user.dto.LoginResult;
import com.mju.Jumoney.domain.user.dto.TokenRefreshResponse;
import com.mju.Jumoney.domain.user.service.AuthService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.JwtProperties;
import com.mju.Jumoney.global.jwt.JwtTokenProvider;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.logging.ApiAccessLoggingInterceptor;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AUTH", description = "AUTH API")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;

    // 카카오 로그인 콜백 (프론트엔드에서 카카오 인가 코드를 전달받아 처리)
    @Operation(summary = "카카오 로그인", description = "카카오에서 발급받은 인가 코드(code)를 이용해 로그인을 진행합니다.")
    @GetMapping("/kakao/login")
    public ResponseEntity<AuthLoginResponse> kakaoLogin(
            HttpServletRequest request,
            @RequestParam("code") String authorizationCode,
            @RequestParam("redirectUri") String redirectUri
    ) {
        log.info("[AuthController] 카카오 로그인 요청 수신");

        // 1. AuthService를 통해 카카오 로그인 진행 및 JWT 발급
        LoginResult loginResult = authService.loginWithKakao(authorizationCode, redirectUri);

        // 2. Refresh Token을 담을 HttpOnly + Secure 쿠키 생성
        ResponseCookie cookie = createRefreshTokenCookie(loginResult.refreshToken());
        markActivityUser(request, loginResult.responseDto().userId());

        log.info("[AuthController] 프론트엔드로 AccessToken(Body) 및 RefreshToken(Cookie) 반환 완료");

        // 3. 응답 반환
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResult.responseDto());
    }

    @Operation(summary = "토큰 재발급", description = "쿠키에 담긴 Refresh Token을 확인하여 새로운 토큰을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(
            HttpServletRequest request,
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null) {
            throw new RuntimeException("Refresh Token 쿠키가 존재하지 않습니다.");
        }

        Map<String, String> tokens = authService.reissueTokens(refreshToken);
        markActivityUser(request, jwtTokenProvider.getUserIdFromToken(tokens.get("accessToken")));

        ResponseCookie cookie = createRefreshTokenCookie(tokens.get("refreshToken"));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new TokenRefreshResponse(tokens.get("accessToken")));
    }

    @Operation(summary = "개발자용 임시 로그인", description = "카카오 연동 없이 닉네임만으로 토큰을 즉시 발급합니다. (테스트용)")
    @PostMapping("/dev/login")
    public ResponseEntity<AuthLoginResponse> devLogin(
            HttpServletRequest request,
            @RequestParam("nickname") String nickname
    ) {
        Map<String, Object> result = authService.devLogin(nickname);

        ResponseCookie cookie = createRefreshTokenCookie((String) result.get("refreshToken"));
        markActivityUser(request, (Long) result.get("userId"));

        AuthLoginResponse responseBody = new AuthLoginResponse(
                (String) result.get("accessToken"),
                (Long) result.get("userId"),
                (String) result.get("nickname"),
                false
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(responseBody);
    }

    @Operation(summary = "로그아웃", description = "로그인 사용자의 Refresh Token을 무효화하고 쿠키를 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {

        Long userId = getOptionalUserId(userPrincipal);
        if (userId == null && refreshToken != null) {
            try {
                userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            } catch (CustomException ignored) {
                userId = null;
            }
        }
        if (userId != null) {
            markActivityUser(request, userId);
        }

        authService.logout(getOptionalUserId(userPrincipal), refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createDeleteRefreshTokenCookie().toString())
                .body(ApiResponse.success(SuccessCode.OK));
    }

    // 쿠키 생성 공통 로직
    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        long maxAgeSeconds = jwtProperties.getRefreshTokenValidity() / 1000;
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie createDeleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }

    private Long getOptionalUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return null;
        }
        return userPrincipal.userId();
    }

    private void markActivityUser(HttpServletRequest request, Long userId) {
        ApiAccessLoggingInterceptor.RequestContextHolder.setUserLabel(request, userId + "번 사용자");
    }
}
