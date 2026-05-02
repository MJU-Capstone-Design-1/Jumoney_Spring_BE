package com.mju.Jumoney.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// JWT 예외 발생 시 JSON으로 응답하는 필터 (SecurityConfig에서 직접 생성)
@Slf4j
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            log.warn("JWT 예외 처리: code={}, uri={}", e.getErrorCode().getCode(), request.getRequestURI());
            sendErrorResponse(response, e);
        }
    }

    private void sendErrorResponse(HttpServletResponse response, CustomException e) throws IOException {
        response.setStatus(e.getErrorCode().getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<Void> errorResponse = ApiResponse.error(e.getErrorCode());
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
