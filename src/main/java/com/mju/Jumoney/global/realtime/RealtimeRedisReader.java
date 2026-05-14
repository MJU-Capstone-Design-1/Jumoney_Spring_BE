package com.mju.Jumoney.global.realtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class RealtimeRedisReader {

    private final StringRedisTemplate realtimeStringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RealtimeRedisReader(
            @Qualifier("realtimeStringRedisTemplate") StringRedisTemplate realtimeStringRedisTemplate
    ) {
        this.realtimeStringRedisTemplate = realtimeStringRedisTemplate;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Optional<String> getRaw(String key) {
        return Optional.ofNullable(realtimeStringRedisTemplate.opsForValue().get(key));
    }

    public DataType type(String key) {
        return realtimeStringRedisTemplate.type(key);
    }

    public <T> Optional<T> get(String key, Class<T> clazz) {
        return getRaw(key).flatMap(json -> parse(json, clazz, key));
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        return getRaw(key).flatMap(json -> parse(json, typeReference, key));
    }

    public Optional<String> hashGetRaw(String key, String field) {
        Object value = realtimeStringRedisTemplate.opsForHash().get(key, field);
        return value == null ? Optional.empty() : Optional.of(value.toString());
    }

    public <T> Optional<T> hashGet(String key, String field, Class<T> clazz) {
        return hashGetRaw(key, field).flatMap(json -> parse(json, clazz, key + "#" + field));
    }

    public <T> Optional<T> hashGet(String key, String field, TypeReference<T> typeReference) {
        return hashGetRaw(key, field).flatMap(json -> parse(json, typeReference, key + "#" + field));
    }

    public Set<String> zSetRangeRaw(String key, long start, long end) {
        Set<String> values = realtimeStringRedisTemplate.opsForZSet().range(key, start, end);
        return values == null ? Collections.emptySet() : values;
    }

    public Set<String> zSetReverseRangeRaw(String key, long start, long end) {
        Set<String> values = realtimeStringRedisTemplate.opsForZSet().reverseRange(key, start, end);
        return values == null ? Collections.emptySet() : values;
    }

    private <T> Optional<T> parse(String json, Class<T> clazz, String source) {
        try {
            return Optional.of(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.error("Realtime Redis JSON parse error: source={}, targetClass={}", source, clazz.getSimpleName(), e);
            return Optional.empty();
        }
    }

    private <T> Optional<T> parse(String json, TypeReference<T> typeReference, String source) {
        try {
            return Optional.of(objectMapper.readValue(json, typeReference));
        } catch (JsonProcessingException e) {
            log.error("Realtime Redis JSON parse error: source={}", source, e);
            return Optional.empty();
        }
    }
}
