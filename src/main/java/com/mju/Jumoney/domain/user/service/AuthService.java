package com.mju.Jumoney.domain.user.service;

import com.mju.Jumoney.domain.user.domain.User;
import com.mju.Jumoney.domain.user.dto.AuthLoginResponse;
import com.mju.Jumoney.domain.user.dto.LoginResult;
import com.mju.Jumoney.domain.user.enums.AuthProvider;
import com.mju.Jumoney.domain.user.enums.Role;
import com.mju.Jumoney.domain.user.exception.UserErrorCode;
import com.mju.Jumoney.domain.user.repository.UserRepository;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.JwtProperties;
import com.mju.Jumoney.global.jwt.JwtTokenProvider;
import com.mju.Jumoney.global.oauth2.KakaoClient;
import com.mju.Jumoney.global.oauth2.KakaoUserInfoResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 인증 로직을 총괄하는 서비스 (카카오 API 통신, 유저 가입 및 복구 처리, JWT 생성 및 Redis 저장 등)
public class AuthService {

    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RedisUtil redisUtil;

    // 카카오 로그인 및 자동 회원가입
    @Transactional
    public LoginResult loginWithKakao(String authorizationCode, String redirectUri) {
        log.info("[AuthService] 카카오 로그인 처리 시작");

        // 1. 카카오 토큰 발급 및 사용자 정보 조회
        String kakaoAccessToken = kakaoClient.getAccessToken(authorizationCode, redirectUri);
        KakaoUserInfoResponse userInfo = kakaoClient.getUserInfo(kakaoAccessToken);

        String providerId = String.valueOf(userInfo.getId());
        boolean isNewMember = false;

        // 2. 회원 조회
        Optional<User> optionalUser = userRepository.findByProviderAndProviderIdIncludeDeleted(AuthProvider.KAKAO.name(), providerId);
        User user;

        if (optionalUser.isEmpty()) {
            // 완전 신규 가입
            log.info("[AuthService] 신규 가입 진행 - 카카오 ID: {}", providerId);
            isNewMember = true;
            user = userRepository.save(User.of(AuthProvider.KAKAO, providerId, userInfo.getNickname()));
        } else {
            // 기존 회원 로그인
            user = optionalUser.get();
            log.info("[AuthService] 기존 회원 로그인 - User ID: {}", user.getId());

            // 탈퇴했던 회원이면 복구 처리
            if (user.getDeletedAt() != null) {
                log.info("[AuthService] 탈퇴 계정 복구 처리 - User ID: {}", user.getId());
                user.restore();
            }
        }

        // 3. JWT 발급 (Access, Refresh)
        String role = user.getRole().name();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), role);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), role);

        // 4. Refresh Token을 DB가 아닌 Redis에 저장 (성능 최적화)
        long refreshTokenValidity = jwtProperties.getRefreshTokenValidity();
        redisUtil.save("RT:" + user.getId(), refreshToken, java.time.Duration.ofMillis(refreshTokenValidity));

        // 5. 컨트롤러에 전달할 응답 결과 생성
        AuthLoginResponse responseDto = new AuthLoginResponse(accessToken, user.getId(), user.getNickname(), isNewMember);
        return new LoginResult(responseDto, refreshToken);
    }

    // 토큰 재발급 (Refresh Token Rotation 적용)
    @Transactional
    public Map<String, String> reissueTokens(String refreshToken) {
        // Refresh Token 검증
        jwtTokenProvider.validateToken(refreshToken);

        // 토큰에서 유저 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // Redis에 저장된 RT와 일치하는지 확인 (탈취 방지)
        String redisToken = redisUtil.get("RT:" + userId, String.class).orElse(null);
        if (redisToken == null || !redisToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 유저 존재 여부 검증
        User user = findUserById(userId);

        // 새로운 Access, Refresh Token 생성 (RTR)
        String role = user.getRole().name();
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId(), role);

        // Redis 업데이트
        redisUtil.save("RT:" + user.getId(), newRefreshToken, java.time.Duration.ofMillis(jwtProperties.getRefreshTokenValidity()));

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        );
    }

    // 프론트 없이 백엔드 혼자 테스트하기 위한 임시 발급 API (테스트용)
    @Transactional
    public Map<String, Object> devLogin(String nickname) {
        // 닉네임으로 유저를 찾고, 없으면 임시 가입 처리
        User user = userRepository.findByNickname(nickname)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .nickname(nickname)
                                .provider(AuthProvider.KAKAO)
                                .providerId("DEV_" + UUID.randomUUID().toString()) // 가짜 카카오 ID
                                .role(Role.USER)
                                .build()
                ));

        String role = user.getRole().name();
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), role);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), role);

        redisUtil.save("RT:" + user.getId(), refreshToken, java.time.Duration.ofMillis(jwtProperties.getRefreshTokenValidity()));

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", user.getId(),
                "nickname", user.getNickname()
        );
    }

    // ========== 조회 메서드 ==========
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
    }
}
