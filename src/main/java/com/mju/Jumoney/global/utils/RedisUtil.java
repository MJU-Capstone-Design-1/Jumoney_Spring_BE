package com.mju.Jumoney.global.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate; // Redis에서 문자열 가져옴
    private final ObjectMapper objectMapper; // 자바 객체 <-> JSON 변환

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;

        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // ========== Key-Value (String) 연산 ==========
    // 자바 객체 -> JSON (TTL 있는 경우)
    public <T> void save(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.error("Redis save error: key={}", key, e);
        }
    }

    // 자바 객체 -> JSON (TTL 없는 경우)
    public <T> void save(String key, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            stringRedisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            log.error("Redis save error: key={}", key, e);
        }
    }

    // JSON -> 자바 객체
    // 단일 객체 조회용
    public <T> Optional<T> get(String key, Class<T> clazz) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.error("Redis get error: key={}, targetClass={}", key, clazz.getSimpleName(), e);
            return Optional.empty();
        }
    }

    // JSON -> 자바 객체
    // 리스트/컬렉션 조회용
    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, typeReference));
        } catch (JsonProcessingException e) {
            log.error("Redis get list error: key={}", key, e);
            return Optional.empty();
        }
    }

    // ========== ZSET (Sorted Set) 연산 ==========
    // ZSET에 데이터 추가 (score 기준으로 자동 정렬됨)
    public <T> void addToZSet(String key, T value, double score) {
        try {
            String json = objectMapper.writeValueAsString(value);
            // 일반 set이 아니라 ZSet의 add 사용 (데이터와 함께 score 전달)
            stringRedisTemplate.opsForZSet().add(key, json, score);
        } catch (JsonProcessingException e) {
            log.error("Redis ZSET save error: key={}", key, e);
        }
    }

    // ZSET에서 데이터 조회
    public <T> Set<T> getZSetRange(String key, long start, long end, Class<T> clazz) {
        // ZSET에서 특정 범위의 문자열 꺼내오기
        Set<String> jsonSet = stringRedisTemplate.opsForZSet().range(key, start, end);
        if (jsonSet == null || jsonSet.isEmpty()) return Collections.emptySet();

        // 꺼내 온 문자열을 자바 객체로 바꿔 전달
        return jsonSet.stream()
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, clazz);
                    } catch (JsonProcessingException e) {
                        log.error("Redis ZSET parse error: key={}, json={}", key, json, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    // ========== 공통 유틸리티 메서드 ==========
    // Redis에서 해당 데이터 삭제
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    // Redis에 해당 데이터 있는지 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
}