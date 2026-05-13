package com.mju.Jumoney.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.password}")
    private String password;
    @Value("${realtime.redis.host}")
    private String realtimeHost;
    @Value("${realtime.redis.port}")
    private int realtimePort;
    @Value("${realtime.redis.password:}")
    private String realtimePassword;

    // Redis 연결 팩토리 (Lettuce)
    // Lettuce를 사용하여 비동기(Non-blocking)로 동작
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        // 운영 환경에서는 비밀번호 필요
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }

    // Node 서버가 적재한 실시간 피드 전용 Redis 연결.
    @Bean("realtimeRedisConnectionFactory")
    public RedisConnectionFactory realtimeRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(realtimeHost, realtimePort);
        if (realtimePassword != null && !realtimePassword.isBlank()) {
            config.setPassword(realtimePassword);
        }
        return new LettuceConnectionFactory(config);
    }

    // 앱 내부 Redis 전용 StringRedisTemplate.
    // JWT refresh token, REST fallback 캐시값 등을 저장합니다.
    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean("realtimeStringRedisTemplate")
    public StringRedisTemplate realtimeStringRedisTemplate(
            @Qualifier("realtimeRedisConnectionFactory") RedisConnectionFactory connectionFactory
    ) {
        return new StringRedisTemplate(connectionFactory);
    }

    // 스프링 부트 4.0.3 버전에서는 GenericJackson2JsonRedisSerializer 지원 중지
    // 모든 통신을 StringRedisTemplate로 통일하고 JSON 문자열로 바꾸는 작업은 비즈니스 계층에서 수행하도록 변경

    /*// 객체 저장용 범용 RedisTemplate
    // Java 객체(DTO, ZSET 등)를 Redis에 한번에 넣고 뺄 때 사용
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 직렬화: String 타입으로 저장
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value 직렬화: JSON 포맷으로 저장
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        return template;
    }

    // Redis 전용 커스텀 ObjectMapper
    // 글로벌(@Bean)로 등록하면 Spring MVC의 기본 JSON 설정이 오염되므로 private으로 격리
    private ObjectMapper redisObjectMapper() {
        // 보안 검증기: JSON 역직렬화 시, 악의적인 클래스(RCE 공격 등)가 실행되지 않도록
        // 안전한 Object의 서브타입들만 허용하는 방화벽 역할
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // 날짜를 "2026-04-29T12:00:00" 같은 사람이 읽을 수 있는 ISO-8601 문자열 형태로 저장
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // JSON 저장 시 클래스 타입 정보를 함께 저장 (캐스팅 에러 없이 정확한 객체로 매핑되도록)
                .activateDefaultTyping(typeValidator, ObjectMapper.DefaultTyping.NON_FINAL);
    }*/
}
