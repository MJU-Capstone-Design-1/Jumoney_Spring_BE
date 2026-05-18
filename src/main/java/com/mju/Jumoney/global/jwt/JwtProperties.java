package com.mju.Jumoney.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
//application.yml 파일의 jwt으로 시작하는 모든 설정값을 필드에 주입 -> 자바 객체로 바인딩
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secretKey;
    private long accessTokenValidity;
    private long refreshTokenValidity;
    private boolean cookieSecure;
    private String cookieSameSite;
}
