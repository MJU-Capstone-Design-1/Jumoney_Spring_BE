package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncFailureResponse;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncResponse;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncStatusResponse;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import com.mju.Jumoney.domain.stock.repository.StockCandleRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stock.utils.StockCandleTimeUtil;
import com.mju.Jumoney.global.batch.MarketCalendarService;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.chart.KisMinuteCandleMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StockMinuteCandleSyncService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int FINALIZATION_BUFFER_MINUTES = 2;
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(15, 30);
    private static final int KIS_MINUTE_CHART_MAX_COUNT = 30;
    private static final int KIS_DAILY_MINUTE_CHART_MAX_COUNT = 120;

    private final KisApiClient kisApiClient;
    private final StockRepository stockRepository;
    private final StockCandleRepository stockCandleRepository;
    private final MarketCalendarService marketCalendarService;

    public MinuteCandleSyncResponse syncTodayMinuteCandles(String stockCode) {
        return syncMinuteCandles(stockCode, LocalDate.now(KOREA_ZONE));
    }

    public MinuteCandleSyncResponse syncMinuteCandles(String stockCode, LocalDate tradingDate) {
        String normalizedStockCode = StringUtils.hasText(stockCode) ? stockCode.trim() : null;
        validateTradingDate(tradingDate);
        LocalDateTime requestedAt = LocalDateTime.now(KOREA_ZONE).truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime finalizationCutoffTime = resolveFinalizationCutoffTime(tradingDate, requestedAt);
        List<Stock> targetStocks = resolveTargetStocks(normalizedStockCode);
        List<MinuteCandleSyncFailureResponse> failures = new ArrayList<>();

        int successCount = 0;
        int savedCandleCount = 0;
        int skippedRecentCandleCount = 0;
        int kisRequestCount = 0;

        for (Stock stock : targetStocks) {
            try {
                SyncStockResult result = syncStock(stock, tradingDate, finalizationCutoffTime);
                successCount++;
                savedCandleCount += result.savedCandleCount();
                skippedRecentCandleCount += result.skippedRecentCandleCount();
                kisRequestCount += result.kisRequestCount();
            } catch (Exception e) {
                failures.add(new MinuteCandleSyncFailureResponse(
                        stock.getStockCode(),
                        stock.getName(),
                        e.getMessage()
                ));
            }
        }

        return new MinuteCandleSyncResponse(
                normalizedStockCode,
                requestedAt,
                finalizationCutoffTime,
                FINALIZATION_BUFFER_MINUTES,
                targetStocks.size(),
                kisRequestCount,
                successCount,
                failures.size(),
                savedCandleCount,
                skippedRecentCandleCount,
                failures
        );
    }

    public MinuteCandleSyncStatusResponse getTodayMinuteCandleSyncStatus(String stockCode, LocalDate date) {
        String normalizedStockCode = StringUtils.hasText(stockCode) ? stockCode.trim() : null;
        if (!StringUtils.hasText(normalizedStockCode)) {
            throw new IllegalArgumentException("종목 코드가 필요합니다.");
        }

        Stock stock = stockRepository.findByStockCode(normalizedStockCode)
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 종목 코드입니다. stockCode=" + normalizedStockCode));
        LocalDate targetDate = date == null ? LocalDate.now(KOREA_ZONE) : date;
        LocalDateTime dbExpectedStartTime = LocalDateTime.of(targetDate, MARKET_OPEN_TIME);
        LocalDateTime dbExpectedEndTime = resolveExpectedDbEndTime(targetDate);
        long dbExpectedCandleCount = calculateExpectedCandleCount(dbExpectedStartTime, dbExpectedEndTime);

        long candleCount = stockCandleRepository.countByStockIdAndIntervalTypeAndCandleTimeBetween(
                stock.getId(),
                StockCandleIntervalType.MINUTE,
                dbExpectedStartTime,
                dbExpectedEndTime
        );
        LocalDateTime firstCandleTime = stockCandleRepository.findFirstByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                        stock.getId(),
                        StockCandleIntervalType.MINUTE,
                        dbExpectedStartTime,
                        dbExpectedEndTime
                )
                .map(StockCandle::getCandleTime)
                .orElse(null);
        LocalDateTime lastCandleTime = stockCandleRepository.findFirstByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeDesc(
                        stock.getId(),
                        StockCandleIntervalType.MINUTE,
                        dbExpectedStartTime,
                        dbExpectedEndTime
                )
                .map(StockCandle::getCandleTime)
                .orElse(null);

        boolean hasAnyCandle = candleCount > 0;
        boolean hasExpectedCandleCount = candleCount >= dbExpectedCandleCount;
        boolean coversExpectedRange = hasAnyCandle
                && !firstCandleTime.isAfter(dbExpectedStartTime)
                && !lastCandleTime.isBefore(dbExpectedEndTime);
        LocalDateTime realtimeExpectedStartTime = resolveRealtimeExpectedStartTime(targetDate, dbExpectedEndTime);
        LocalDateTime realtimeExpectedEndTime = resolveRealtimeExpectedEndTime(targetDate);
        boolean realtimeCheckRequired = realtimeExpectedStartTime != null
                && realtimeExpectedEndTime != null
                && !realtimeExpectedStartTime.isAfter(realtimeExpectedEndTime);

        return new MinuteCandleSyncStatusResponse(
                stock.getStockCode(),
                stock.getName(),
                targetDate,
                dbExpectedStartTime,
                dbExpectedEndTime,
                dbExpectedCandleCount,
                candleCount,
                firstCandleTime,
                lastCandleTime,
                hasAnyCandle,
                hasExpectedCandleCount,
                coversExpectedRange,
                realtimeCheckRequired,
                realtimeExpectedStartTime,
                realtimeExpectedEndTime,
                false,
                "실시간 분봉 Redis key 규격이 아직 정해지지 않아 Spring에서는 DB 확정 구간만 검증합니다."
        );
    }

    private List<Stock> resolveTargetStocks(String stockCode) {
        if (StringUtils.hasText(stockCode)) {
            Stock stock = stockRepository.findByStockCode(stockCode)
                    .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 종목 코드입니다. stockCode=" + stockCode));
            return List.of(stock);
        }
        return stockRepository.findAll();
    }

    private SyncStockResult syncStock(Stock stock, LocalDate tradingDate, LocalDateTime finalizationCutoffTime) {
        int savedCount = 0;
        int skippedRecentCount = 0;
        int kisRequestCount = 0;
        LocalDateTime syncStartTime = resolveSyncStartTime(stock.getId(), finalizationCutoffTime);
        boolean isTodaySync = tradingDate.equals(LocalDate.now(KOREA_ZONE));
        Set<LocalDateTime> affectedThirtyMinuteBuckets = new HashSet<>();

        for (LocalTime inputTime : resolveInputTimes(syncStartTime, finalizationCutoffTime, isTodaySync ? KIS_MINUTE_CHART_MAX_COUNT : KIS_DAILY_MINUTE_CHART_MAX_COUNT)) {
            kisRequestCount++;
            List<KisMinuteCandleMetrics> candles = loadMinuteCandles(stock.getStockCode(), tradingDate, inputTime, isTodaySync)
                    .stream()
                    .filter(this::isValidCandle)
                    .sorted(Comparator.comparing(KisMinuteCandleMetrics::candleTime))
                    .toList();

            List<KisMinuteCandleMetrics> candlesToSave = new ArrayList<>();
            for (KisMinuteCandleMetrics candle : candles) {
                if (candle.candleTime().isAfter(finalizationCutoffTime)) {
                    skippedRecentCount++;
                    continue;
                }
                if (candle.candleTime().isBefore(syncStartTime)) {
                    continue;
                }
                if (candle.candleTime().toLocalTime().isBefore(MARKET_OPEN_TIME)) {
                    continue;
                }

                candlesToSave.add(candle);
            }

            if (candlesToSave.isEmpty()) {
                continue;
            }

            Map<LocalDateTime, StockCandle> existingCandleMap = loadExistingCandleMap(stock.getId(), candlesToSave);
            for (KisMinuteCandleMetrics candle : candlesToSave) {
                upsertFinalMinuteCandle(stock, candle, existingCandleMap.get(candle.candleTime()));
                affectedThirtyMinuteBuckets.add(StockCandleTimeUtil.toThirtyMinuteBucketStart(candle.candleTime()));
                savedCount++;
            }
        }

        upsertThirtyMinuteCandles(stock, affectedThirtyMinuteBuckets, finalizationCutoffTime);
        return new SyncStockResult(savedCount, skippedRecentCount, kisRequestCount);
    }

    private LocalDateTime resolveSyncStartTime(Long stockId, LocalDateTime finalizationCutoffTime) {
        LocalDateTime marketOpenTime = LocalDateTime.of(finalizationCutoffTime.toLocalDate(), MARKET_OPEN_TIME);
        return stockCandleRepository.findFirstByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeDesc(
                        stockId,
                        StockCandleIntervalType.MINUTE,
                        marketOpenTime,
                        finalizationCutoffTime
                )
                .map(lastSavedCandle -> lastSavedCandle.getCandleTime().plusMinutes(1))
                .orElse(marketOpenTime);
    }

    private List<LocalTime> resolveInputTimes(LocalDateTime syncStartTime, LocalDateTime finalizationCutoffTime, int maxCountPerRequest) {
        if (syncStartTime.isAfter(finalizationCutoffTime)) {
            return List.of();
        }

        LocalTime endTime = finalizationCutoffTime.toLocalTime();
        if (endTime.isBefore(MARKET_OPEN_TIME)) {
            return List.of();
        }

        List<LocalTime> inputTimes = new ArrayList<>();
        LocalTime current = endTime;
        LocalTime startTime = syncStartTime.toLocalTime();
        while (!current.isBefore(MARKET_OPEN_TIME)) {
            inputTimes.add(current);
            if (!current.minusMinutes(maxCountPerRequest - 1).isAfter(startTime)) {
                break;
            }
            current = current.minusMinutes(maxCountPerRequest);
        }
        return inputTimes;
    }

    private List<KisMinuteCandleMetrics> loadMinuteCandles(String stockCode, LocalDate tradingDate, LocalTime inputTime, boolean isTodaySync) {
        if (isTodaySync) {
            return kisApiClient.getTodayMinuteCandles(stockCode, inputTime);
        }
        return kisApiClient.getDailyMinuteCandles(stockCode, tradingDate, inputTime);
    }

    private LocalDateTime resolveExpectedDbEndTime(LocalDate targetDate) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        if (targetDate.isBefore(today)) {
            return LocalDateTime.of(targetDate, MARKET_CLOSE_TIME);
        }
        if (targetDate.isAfter(today)) {
            return LocalDateTime.of(targetDate, MARKET_OPEN_TIME);
        }

        return resolveDbSyncEndTime(LocalDateTime.now(KOREA_ZONE));
    }

    private LocalDateTime resolveDbSyncEndTime(LocalDateTime requestedAt) {
        LocalDateTime bufferedTime = requestedAt.minusMinutes(FINALIZATION_BUFFER_MINUTES);
        LocalDateTime marketOpen = LocalDateTime.of(bufferedTime.toLocalDate(), MARKET_OPEN_TIME);
        LocalDateTime marketClose = LocalDateTime.of(bufferedTime.toLocalDate(), MARKET_CLOSE_TIME);
        if (bufferedTime.isBefore(marketOpen)) {
            return marketOpen;
        }
        if (bufferedTime.isAfter(marketClose)) {
            return marketClose;
        }

        int minute = bufferedTime.getMinute();
        int flooredMinute = minute >= 30 ? 30 : 0;
        return bufferedTime
                .withMinute(flooredMinute)
                .withSecond(0)
                .withNano(0);
    }

    private LocalDateTime resolveFinalizationCutoffTime(LocalDate tradingDate, LocalDateTime requestedAt) {
        LocalDate today = LocalDate.now(KOREA_ZONE);
        if (tradingDate.isBefore(today)) {
            return LocalDateTime.of(tradingDate, MARKET_CLOSE_TIME);
        }
        if (tradingDate.isAfter(today)) {
            throw new IllegalArgumentException("미래 영업일은 동기화할 수 없습니다. tradingDate=" + tradingDate);
        }
        return resolveDbSyncEndTime(requestedAt);
    }

    private void validateTradingDate(LocalDate tradingDate) {
        if (!marketCalendarService.isOpenDay(tradingDate, KOREA_ZONE)) {
            throw new IllegalArgumentException("휴장일 또는 주말은 동기화할 수 없습니다. tradingDate=" + tradingDate);
        }
    }

    private LocalDateTime resolveRealtimeExpectedStartTime(LocalDate targetDate, LocalDateTime dbExpectedEndTime) {
        if (!targetDate.equals(LocalDate.now(KOREA_ZONE))) {
            return null;
        }
        if (!dbExpectedEndTime.toLocalTime().isBefore(MARKET_CLOSE_TIME)) {
            return null;
        }
        return dbExpectedEndTime.plusMinutes(1);
    }

    private LocalDateTime resolveRealtimeExpectedEndTime(LocalDate targetDate) {
        if (!targetDate.equals(LocalDate.now(KOREA_ZONE))) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime marketOpen = LocalDateTime.of(targetDate, MARKET_OPEN_TIME);
        LocalDateTime marketClose = LocalDateTime.of(targetDate, MARKET_CLOSE_TIME);
        if (now.isBefore(marketOpen)) {
            return null;
        }
        return now.isAfter(marketClose) ? marketClose : now;
    }

    private long calculateExpectedCandleCount(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime)) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(startTime, endTime) + 1;
    }

    private boolean isValidCandle(KisMinuteCandleMetrics candle) {
        return candle.candleTime() != null
                && candle.openPrice() != null
                && candle.highPrice() != null
                && candle.lowPrice() != null
                && candle.closePrice() != null
                && candle.volume() != null;
    }

    private Map<LocalDateTime, StockCandle> loadExistingCandleMap(Long stockId, List<KisMinuteCandleMetrics> candles) {
        LocalDateTime startTime = candles.get(0).candleTime();
        LocalDateTime endTime = candles.get(candles.size() - 1).candleTime();
        Map<LocalDateTime, StockCandle> existingCandleMap = new HashMap<>();

        for (StockCandle existingCandle : stockCandleRepository.findByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                stockId,
                StockCandleIntervalType.MINUTE,
                startTime,
                endTime
        )) {
            existingCandleMap.put(existingCandle.getCandleTime(), existingCandle);
        }
        return existingCandleMap;
    }

    private void upsertFinalMinuteCandle(Stock stock, KisMinuteCandleMetrics candle, StockCandle existingCandle) {
        if (existingCandle != null) {
            existingCandle.updateFinalCandle(
                    candle.openPrice(),
                    candle.highPrice(),
                    candle.lowPrice(),
                    candle.closePrice(),
                    candle.volume(),
                    candle.tradeAmount()
            );
            stockCandleRepository.save(existingCandle);
            return;
        }

        stockCandleRepository.save(StockCandle.createMinute(
                stock,
                candle.candleTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume(),
                candle.tradeAmount()
        ));
    }

    private void upsertThirtyMinuteCandles(Stock stock, Set<LocalDateTime> bucketStartTimes, LocalDateTime finalizationCutoffTime) {
        for (LocalDateTime bucketStartTime : bucketStartTimes) {
            LocalDateTime marketCloseTime = LocalDateTime.of(bucketStartTime.toLocalDate(), MARKET_CLOSE_TIME);
            LocalDateTime bucketEndTime = bucketStartTime.plusMinutes(29).isAfter(marketCloseTime)
                    ? marketCloseTime
                    : bucketStartTime.plusMinutes(29);
            if (bucketEndTime.isAfter(finalizationCutoffTime)) {
                continue;
            }

            List<StockCandle> minuteCandles = stockCandleRepository.findByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                    stock.getId(),
                    StockCandleIntervalType.MINUTE,
                    bucketStartTime,
                    bucketEndTime
            );
            if (minuteCandles.isEmpty()) {
                continue;
            }
            long expectedMinuteCount = calculateExpectedCandleCount(bucketStartTime, bucketEndTime);
            if (minuteCandles.size() < expectedMinuteCount) {
                continue;
            }

            StockCandle existingThirtyMinuteCandle = stockCandleRepository.findByStockIdAndIntervalTypeAndCandleTime(
                    stock.getId(),
                    StockCandleIntervalType.THIRTY_MINUTE,
                    bucketStartTime
            ).orElse(null);

            BigDecimal openPrice = minuteCandles.get(0).getOpenPrice();
            BigDecimal highPrice = minuteCandles.stream()
                    .map(StockCandle::getHighPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(null);
            BigDecimal lowPrice = minuteCandles.stream()
                    .map(StockCandle::getLowPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
            BigDecimal closePrice = minuteCandles.get(minuteCandles.size() - 1).getClosePrice();
            Long volume = minuteCandles.stream()
                    .map(StockCandle::getVolume)
                    .filter(Objects::nonNull)
                    .reduce(0L, Long::sum);
            Long tradeAmount = minuteCandles.get(minuteCandles.size() - 1).getTradeAmount();

            if (existingThirtyMinuteCandle != null) {
                existingThirtyMinuteCandle.updateFinalCandle(openPrice, highPrice, lowPrice, closePrice, volume, tradeAmount);
                stockCandleRepository.save(existingThirtyMinuteCandle);
                continue;
            }

            stockCandleRepository.save(StockCandle.createFinal(
                    stock,
                    StockCandleIntervalType.THIRTY_MINUTE,
                    bucketStartTime,
                    openPrice,
                    highPrice,
                    lowPrice,
                    closePrice,
                    volume,
                    tradeAmount
            ));
        }
    }

    private record SyncStockResult(
            int savedCandleCount,
            int skippedRecentCandleCount,
            int kisRequestCount
    ) {
    }
}
