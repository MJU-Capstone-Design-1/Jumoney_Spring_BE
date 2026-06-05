package com.mju.Jumoney.global.logging;

import com.mju.Jumoney.global.jwt.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ApiAccessLoggingInterceptorTest {

    private final ApiAccessLoggingInterceptor interceptor = new ApiAccessLoggingInterceptor();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preHandleStoresRequestStartTime() {
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/mock-investments/dashboard");
        var response = new org.springframework.mock.web.MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(request.getAttribute("apiAccessLoggingInterceptor.requestStartTime")).isInstanceOf(Long.class);
    }

    @Test
    void afterCompletionLogsUsingMappedPatternAndAuthenticatedUser() throws Exception {
        var request = new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/mock-investments/dashboard");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        request.setAttribute(ApiAccessLoggingInterceptor.REQUEST_START_TIME_ATTRIBUTE, System.currentTimeMillis() - 25);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/mock-investments/dashboard");

        UserPrincipal principal = new UserPrincipal(7L, "USER", "테스터");
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Method method = TestController.class.getDeclaredMethod("dashboard");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

        interceptor.afterCompletion(request, response, handlerMethod, null);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        assertThat(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
                .isEqualTo("/api/mock-investments/dashboard");
    }

    @Test
    void requestScopedUserLabelOverridesSecurityContext() throws Exception {
        var request = new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/auth/dev/login");
        var response = new org.springframework.mock.web.MockHttpServletResponse();
        request.setAttribute(ApiAccessLoggingInterceptor.REQUEST_START_TIME_ATTRIBUTE, System.currentTimeMillis() - 10);
        ApiAccessLoggingInterceptor.RequestContextHolder.setUserLabel(request, "15번 사용자");

        Method method = TestController.class.getDeclaredMethod("dashboard");
        HandlerMethod handlerMethod = new HandlerMethod(new TestController(), method);

        interceptor.afterCompletion(request, response, handlerMethod, null);
        assertThat(request.getAttribute(ApiAccessLoggingInterceptor.ACTIVITY_USER_LABEL_ATTRIBUTE))
                .isEqualTo("15번 사용자");
    }

    static class TestController {
        public void dashboard() {
        }
    }
}
