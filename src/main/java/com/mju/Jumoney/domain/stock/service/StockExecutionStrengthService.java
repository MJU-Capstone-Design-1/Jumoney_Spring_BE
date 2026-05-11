package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.price.KisExecutionStrengthMetrics;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockExecutionStrengthService {

    private static final String EXECUTION_STRENGTH_KEY_PREFIX = "stock:execution-strength:";

    private final KisApiClient kisApiClient;
    private final RedisUtil redisUtil;

    @Value("${stock.execution-strength.cache-ttl-seconds:180}")
    private long cacheTtlSeconds;

    public Optional<BigDecimal> getExecutionStrength(String stockCode) {
        String key = cacheKey(stockCode);
        Optional<BigDecimal> cached = redisUtil.get(key, BigDecimal.class);
        if (cached.isPresent()) {
            return cached;
        }

        KisExecutionStrengthMetrics metrics = kisApiClient.getExecutionStrength(stockCode);
        BigDecimal executionStrength = metrics.executionStrength();
        if (executionStrength == null) {
            log.warn("[StockExecutionStrength] 체결강도 값이 비어있습니다. stockCode={}", stockCode);
            return Optional.empty();
        }

        redisUtil.save(key, executionStrength, Duration.ofSeconds(cacheTtlSeconds));
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

    private String cacheKey(String stockCode) {
        return EXECUTION_STRENGTH_KEY_PREFIX + stockCode;
    }
}
