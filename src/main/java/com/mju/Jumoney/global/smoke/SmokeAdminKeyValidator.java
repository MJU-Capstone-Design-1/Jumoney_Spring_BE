package com.mju.Jumoney.global.smoke;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SmokeAdminKeyValidator {

    private final Environment environment;

    @Value("${kis.smoke.admin-key:}")
    private String configuredAdminKey;

    public SmokeAdminKeyValidator(Environment environment) {
        this.environment = environment;
    }

    public void validate(String adminKey) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        if (!StringUtils.hasText(configuredAdminKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Smoke admin key is not configured.");
        }
        if (!configuredAdminKey.equals(adminKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid smoke admin key.");
        }
    }
}
