package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.price.KisExecutionStrengthMetrics;
import com.mju.Jumoney.global.realtime.RealtimeRedisReader;
import com.mju.Jumoney.global.realtime.StockRealtimeSnapshot;
import com.mju.Jumoney.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockExecutionStrengthService {

    private static final String EXECUTION_STRENGTH_KEY_PREFIX = "stock:execution-strength:";
    private static final String REALTIME_STOCK_LATEST_KEY_PREFIX = "stock:latest:";

    private final KisApiClient kisApiClient;
    private final RedisUtil redisUtil;
    private final RealtimeRedisReader realtimeRedisReader;

    @Value("${stock.execution-strength.cache-ttl-seconds:180}")
    private long cacheTtlSeconds;
    @Value("${stock.execution-strength.realtime-freshness-seconds:180}")
    private long realtimeFreshnessSeconds;

    public Optional<BigDecimal> getExecutionStrength(String stockCode) {
        Optional<BigDecimal> realtimeExecutionStrength = getRealtimeExecutionStrengthSafely(stockCode);
        if (realtimeExecutionStrength.isPresent()) {
            return realtimeExecutionStrength;
        }

        String key = cacheKey(stockCode);
        Optional<BigDecimal> cached = getCachedExecutionStrengthSafely(key, stockCode);
        if (cached.isPresent()) {
            return cached;
        }

        KisExecutionStrengthMetrics metrics = kisApiClient.getExecutionStrength(stockCode);
        BigDecimal executionStrength = metrics.executionStrength();
        if (executionStrength == null) {
            log.warn("[StockExecutionStrength] 체결강도 값이 비어있습니다. stockCode={}", stockCode);
            return Optional.empty();
        }

        cacheExecutionStrengthSafely(key, executionStrength, stockCode);
        return Optional.of(executionStrength);
    }

    public Map<String, BigDecimal> getExecutionStrengths(Collection<String> stockCodes) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (String stockCode : stockCodes) {
            getExecutionStrength(stockCode)
                    .ifPresent(executionStrength -> result.put(stockCode, executionStrength));
        }
        return result;
    }

    private Optional<BigDecimal> getRealtimeExecutionStrength(String stockCode) {
        return realtimeRedisReader.get(realtimeKey(stockCode), StockRealtimeSnapshot.class)
                .filter(this::isFreshRealtimeSnapshot)
                .map(StockRealtimeSnapshot::strength)
                .filter(Objects::nonNull);
    }

    private Optional<BigDecimal> getRealtimeExecutionStrengthSafely(String stockCode) {
        try {
            return getRealtimeExecutionStrength(stockCode);
        } catch (RuntimeException e) {
            log.warn("[StockExecutionStrength] 실시간 Redis 조회 실패. KIS 조회로 fallback합니다. stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> getCachedExecutionStrengthSafely(String key, String stockCode) {
        try {
            return redisUtil.get(key, BigDecimal.class);
        } catch (RuntimeException e) {
            log.warn("[StockExecutionStrength] 캐시 Redis 조회 실패. KIS 조회로 fallback합니다. stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }

    private void cacheExecutionStrengthSafely(String key, BigDecimal executionStrength, String stockCode) {
        try {
            redisUtil.save(key, executionStrength, Duration.ofSeconds(cacheTtlSeconds));
        } catch (RuntimeException e) {
            log.warn("[StockExecutionStrength] 캐시 Redis 저장 실패. 조회 결과는 캐시하지 않고 반환합니다. stockCode={}", stockCode, e);
        }
    }

    private boolean isFreshRealtimeSnapshot(StockRealtimeSnapshot snapshot) {
        if (snapshot.timestamp() == null || realtimeFreshnessSeconds <= 0) {
            return false;
        }

        long freshnessThresholdMillis = Duration.ofSeconds(realtimeFreshnessSeconds).toMillis();
        long ageMillis = System.currentTimeMillis() - snapshot.timestamp();
        return ageMillis >= 0 && ageMillis <= freshnessThresholdMillis;
    }

    private String cacheKey(String stockCode) {
        return EXECUTION_STRENGTH_KEY_PREFIX + stockCode;
    }

    private String realtimeKey(String stockCode) {
        return REALTIME_STOCK_LATEST_KEY_PREFIX + stockCode;
    }
}
