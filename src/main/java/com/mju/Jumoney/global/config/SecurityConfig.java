package com.mju.Jumoney.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.Jumoney.global.jwt.JwtAuthenticationFilter;
import com.mju.Jumoney.global.jwt.JwtExceptionFilter;
import com.mju.Jumoney.global.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity // Spring Security를 활성화합니다.
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    // Spring Security 메인 필터 체인
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // JWT 인증 및 예외 필터 직접 생성 (이중 등록 방지)
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        JwtExceptionFilter jwtExceptionFilter = new JwtExceptionFilter(objectMapper);

        // Stateless(JWT)를 위한 기본 설정
        http
            // CSRF 보호 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            // 폼 로그인, HTTP Basic 인증 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 세션 관리 정책을 STATELESS로 설정 (서버가 세션을 만들지 않음)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        // CORS 및 헤더 프레임 허용 설정
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        // API 경로별 접근 권한 설정
        http
            .authorizeHttpRequests(authz -> authz
                    // 최신 보안 표준: ignoring() 대신 permitAll() 사용
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/favicon.ico", "/error").permitAll()
                    // 기타 모든 요청 임시 허용
                    .anyRequest().permitAll()
            );
        /*
        // TODO: 추후 인증된 사용자만 허용되도록 코드 교체
        http
            .authorizeHttpRequests(authz -> authz
                    // 로그인, 회원가입 등 인증 API는 누구나 허용
                    .requestMatchers("/api/auth/**", "/oauth2/**").permitAll()
                    // 그 외는 인증된 사용자만 허용
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().authenticated()
            );
        */

        // 커스텀 필터 체인 순서 배치
        // 예외 처리 필터 -> JWT 인증 필터 -> 기본 인증 필터 순서로 등록
        http
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtExceptionFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    // CORS 설정 Bean (프론트엔드 연동)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 해당 주소에서 오는 API 요청을 허용 (로컬용, 운영용)
        config.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173", "https://jumoney.site"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 API 경로에 이 CORS 설정을 적용
        source.registerCorsConfiguration("/**", config); 
        return source;
    }
}
