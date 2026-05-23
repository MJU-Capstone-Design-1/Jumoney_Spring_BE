package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.domain.stock.dto.StockCurrentPriceSnapshot;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.price.KisCurrentPriceMetrics;
import com.mju.Jumoney.global.realtime.RealtimeRedisReader;
import com.mju.Jumoney.global.realtime.StockRealtimeSnapshot;
import com.mju.Jumoney.global.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCurrentPriceService {

    private static final String CURRENT_PRICE_KEY_PREFIX = "stock:current-price:";
    private static final String REALTIME_STOCK_LATEST_KEY_PREFIX = "stock:latest:";

    private final KisApiClient kisApiClient;
    private final RedisUtil redisUtil;
    private final RealtimeRedisReader realtimeRedisReader;

    @Value("${stock.current-price.cache-ttl-seconds:600}")
    private long cacheTtlSeconds;
    @Value("${stock.current-price.realtime-freshness-seconds:180}")
    private long realtimeFreshnessSeconds;

    public Optional<StockCurrentPriceSnapshot> getCurrentPrice(String stockCode) {
        Optional<StockCurrentPriceSnapshot> realtimeCurrentPrice = getRealtimeCurrentPriceSafely(stockCode);
        if (realtimeCurrentPrice.isPresent()) {
            return realtimeCurrentPrice;
        }

        String key = cacheKey(stockCode);
        Optional<StockCurrentPriceSnapshot> cached = getCachedCurrentPriceSafely(key, stockCode);
        if (cached.isPresent()) {
            return cached;
        }

        try {
            KisCurrentPriceMetrics metrics = kisApiClient.getCurrentPrice(stockCode);
            StockCurrentPriceSnapshot snapshot = new StockCurrentPriceSnapshot(
                    metrics.currentPrice(),
                    metrics.changeRate()
            );
            if (!hasDisplayValue(snapshot)) {
                log.warn("[StockCurrentPrice] 현재가 표시 값이 비어있습니다. stockCode={}", stockCode);
                return Optional.empty();
            }

            cacheCurrentPriceSafely(key, snapshot, stockCode);
            return Optional.of(snapshot);
        } catch (RuntimeException e) {
            log.warn("[StockCurrentPrice] 현재가 조회 실패. stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }

    public Map<String, StockCurrentPriceSnapshot> getCurrentPrices(Collection<String> stockCodes) {
        Map<String, StockCurrentPriceSnapshot> result = new LinkedHashMap<>();
        for (String stockCode : stockCodes) {
            if (!result.containsKey(stockCode)) {
                getCurrentPrice(stockCode).ifPresent(snapshot -> result.put(stockCode, snapshot));
            }
        }
        return result;
    }

    private Optional<StockCurrentPriceSnapshot> getRealtimeCurrentPrice(String stockCode) {
        return realtimeRedisReader.get(realtimeKey(stockCode), StockRealtimeSnapshot.class)
                .filter(this::isFreshRealtimeSnapshot)
                .map(snapshot -> new StockCurrentPriceSnapshot(snapshot.close(), snapshot.rate()))
                .filter(this::hasDisplayValue);
    }

    private Optional<StockCurrentPriceSnapshot> getRealtimeCurrentPriceSafely(String stockCode) {
        try {
            return getRealtimeCurrentPrice(stockCode);
        } catch (RuntimeException e) {
            log.warn("[StockCurrentPrice] 실시간 Redis 조회 실패. KIS 조회로 fallback합니다. stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }

    private Optional<StockCurrentPriceSnapshot> getCachedCurrentPriceSafely(String key, String stockCode) {
        try {
            return redisUtil.get(key, StockCurrentPriceSnapshot.class);
        } catch (RuntimeException e) {
            log.warn("[StockCurrentPrice] 캐시 Redis 조회 실패. KIS 조회로 fallback합니다. stockCode={}", stockCode, e);
            return Optional.empty();
        }
    }

    private void cacheCurrentPriceSafely(String key, StockCurrentPriceSnapshot snapshot, String stockCode) {
        try {
            redisUtil.save(key, snapshot, Duration.ofSeconds(cacheTtlSeconds));
        } catch (RuntimeException e) {
            log.warn("[StockCurrentPrice] 캐시 Redis 저장 실패. 조회 결과는 캐시하지 않고 반환합니다. stockCode={}", stockCode, e);
        }
    }

    private boolean isFreshRealtimeSnapshot(StockRealtimeSnapshot snapshot) {
        if (snapshot.minuteTs() == null || realtimeFreshnessSeconds <= 0) {
            return false;
        }

        long freshnessThresholdMillis = Duration.ofSeconds(realtimeFreshnessSeconds).toMillis();
        long ageMillis = System.currentTimeMillis() - snapshot.minuteTs();
        return ageMillis >= 0 && ageMillis <= freshnessThresholdMillis;
    }

    private boolean hasDisplayValue(StockCurrentPriceSnapshot snapshot) {
        return snapshot.currentPrice() != null || snapshot.changeRate() != null;
    }

    private String cacheKey(String stockCode) {
        return CURRENT_PRICE_KEY_PREFIX + stockCode;
    }

    private String realtimeKey(String stockCode) {
        return REALTIME_STOCK_LATEST_KEY_PREFIX + stockCode;
    }
}
