package com.mju.Jumoney.domain.user.controller;

import com.mju.Jumoney.domain.user.dto.AuthLoginResponse;
import com.mju.Jumoney.domain.user.dto.LoginResult;
import com.mju.Jumoney.domain.user.service.AuthService;
import com.mju.Jumoney.global.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    // 카카오 로그인 콜백 (프론트엔드에서 카카오 인가 코드를 전달받아 처리)
    @GetMapping("/kakao/login")
    public ResponseEntity<AuthLoginResponse> kakaoLogin(
            @RequestParam("code") String authorizationCode
    ) {
        log.info("[AuthController] 카카오 로그인 요청 수신");

        // 1. AuthService를 통해 카카오 로그인 진행 및 JWT 발급
        LoginResult loginResult = authService.loginWithKakao(authorizationCode);

        // 2. Refresh Token을 담을 HttpOnly + Secure 쿠키 생성 (환경별 옵션 적용)
        long maxAgeSeconds = jwtProperties.getRefreshTokenValidity() / 1000;
        ResponseCookie cookie = ResponseCookie.from("refreshToken", loginResult.refreshToken())
                .httpOnly(true)          // JS 탈취 방지 (XSS 방어)
                .secure(jwtProperties.isCookieSecure()) // true면 HTTPS 환경에서만 전송
                .sameSite(jwtProperties.getCookieSameSite()) // SameSite=None는 HTTPS만 허용하기에 로컬 환경에서는 낮춰줌
                .path("/")               // 앱 내의 모든 경로에서 쿠키 전송
                .maxAge(maxAgeSeconds)   // 만료 시간 설정
                .build();

        log.info("[AuthController] 프론트엔드로 AccessToken(Body) 및 RefreshToken(Cookie) 반환 완료");

        // 3. 응답 반환: Header에는 쿠키(Refresh), Body에는 JSON(Access 등)
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(loginResult.responseDto());
    }
}
