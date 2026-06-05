package com.mju.Jumoney.global.logging;

import com.mju.Jumoney.global.jwt.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ApiAccessLoggingInterceptor implements HandlerInterceptor {

    public static final String REQUEST_START_TIME_ATTRIBUTE = "apiAccessLoggingInterceptor.requestStartTime";
    public static final String ACTIVITY_USER_LABEL_ATTRIBUTE = "apiAccessLoggingInterceptor.activityUserLabel";
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(REQUEST_START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            @Nullable Exception ex
    ) {
        String timestamp = LocalDateTime.now().format(LOG_TIME_FORMATTER);
        long durationMs = calculateDurationMs(request);
        String userLabel = resolveUserLabel(request);
        String action = resolveAction(handler, request);

        if (ex == null) {
            log.info(
                    "[사용자 활동] 시각={} | {}가 {}를 사용했습니다. 상태={}, 소요시간={}ms",
                    timestamp,
                    userLabel,
                    action,
                    response.getStatus(),
                    durationMs
            );
            return;
        }

        log.warn(
                "[사용자 활동] 시각={} | {}가 {}를 사용했지만 예외가 발생했습니다. 상태={}, 소요시간={}ms, 예외={}",
                timestamp,
                userLabel,
                action,
                response.getStatus(),
                durationMs,
                ex.getClass().getSimpleName()
        );
    }

    private long calculateDurationMs(HttpServletRequest request) {
        Object startTime = request.getAttribute(REQUEST_START_TIME_ATTRIBUTE);
        if (startTime instanceof Long startedAt) {
            return Math.max(0, System.currentTimeMillis() - startedAt);
        }
        return -1L;
    }

    private String resolveUserLabel(HttpServletRequest request) {
        Object requestUserLabel = request.getAttribute(ACTIVITY_USER_LABEL_ATTRIBUTE);
        if (requestUserLabel instanceof String value) {
            return value;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return "비로그인 사용자";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.userId() + "번 사용자";
        }

        return "비로그인 사용자";
    }

    private String resolveAction(Object handler, HttpServletRequest request) {
        if (handler instanceof HandlerMethod handlerMethod) {
            Operation operation = handlerMethod.getMethodAnnotation(Operation.class);
            if (operation != null && !operation.summary().isBlank()) {
                return operation.summary();
            }

            return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
        }

        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String matchedPattern) {
            return request.getMethod() + " " + matchedPattern;
        }

        return request.getMethod() + " " + request.getRequestURI();
    }

    public static class RequestContextHolder {

        private RequestContextHolder() {
        }

        public static void setUserLabel(HttpServletRequest request, String userLabel) {
            request.setAttribute(ACTIVITY_USER_LABEL_ATTRIBUTE, userLabel);
        }

    }
}
