package com.mju.Jumoney.global.jwt;

import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.response.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;

// 토큰 생성(createAccessToken), 검증(validateToken), 인증객체 반환(getAuthentication) 담당하는 핵심 유틸
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String KEY_ROLE = "role";

    private final JwtProperties jwtProperties;
    private SecretKey key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecretKey());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, jwtProperties.getAccessTokenValidity());
    }

    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, jwtProperties.getRefreshTokenValidity());
    }

    private String createToken(Long userId, String role, long validity) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(KEY_ROLE, role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validity))
                .signWith(key)
                .compact();
    }

    // 토근 검증 메서드
    // 검증 실패 시 CustomException throw → JwtExceptionFilter가 JSON으로 응답
    public void validateToken(String token) {
        try {
            getClaims(token);
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("변조되거나 잘못된 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("지원하지 않는 JWT 토큰입니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            log.warn("JWT 클레임이 비어있습니다.");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        Long userId = Long.valueOf(claims.getSubject());
        String role = claims.get(KEY_ROLE, String.class);

        UserPrincipal principal = new UserPrincipal(userId, role);
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role);

        return new UsernamePasswordAuthenticationToken(principal, "", Collections.singleton(authority));
    }

    // RefreshToken 갱신 시 userId 추출용
    public Long getUserIdFromToken(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
