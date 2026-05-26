package com.mju.Jumoney.domain.stock.service;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncFailureResponse;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncResponse;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockMinuteCandleSyncService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final int FINALIZATION_BUFFER_MINUTES = 2;
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(15, 30);
    private static final LocalTime CLOSING_AUCTION_START_TIME = LocalTime.of(15, 20);
    private static final LocalTime CLOSING_AUCTION_LAST_CONTINUOUS_TIME = LocalTime.of(15, 19);
    private static final LocalTime CLOSING_AUCTION_LAST_SYNTHETIC_TIME = LocalTime.of(15, 29);
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
        LocalDateTime syncStartTime = resolveSyncStartTime(stock.getId(), tradingDate, finalizationCutoffTime);
        boolean isTodaySync = tradingDate.equals(LocalDate.now(KOREA_ZONE));
        Set<LocalDateTime> affectedThirtyMinuteBuckets = new HashSet<>();

        for (LocalTime inputTime : resolveInputTimes(syncStartTime, finalizationCutoffTime, isTodaySync ? KIS_MINUTE_CHART_MAX_COUNT : KIS_DAILY_MINUTE_CHART_MAX_COUNT)) {
            kisRequestCount++;
            List<KisMinuteCandleMetrics> rawCandles = loadMinuteCandles(stock.getStockCode(), tradingDate, inputTime, isTodaySync);

            List<KisMinuteCandleMetrics> candles = rawCandles
                    .stream()
                    .filter(this::isValidCandle)
                    .sorted(Comparator.comparing(KisMinuteCandleMetrics::candleTime))
                    .toList();

            List<KisMinuteCandleMetrics> candlesToSave = new ArrayList<>();
            for (KisMinuteCandleMetrics candle : candles) {
                if (!candle.candleTime().toLocalDate().equals(tradingDate)) {
                    continue;
                }
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
            addClosingAuctionSyntheticCandles(stock, tradingDate, finalizationCutoffTime, syncStartTime, candlesToSave);

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

        affectedThirtyMinuteBuckets.addAll(resolveThirtyMinuteBucketStartTimes(tradingDate));
        upsertThirtyMinuteCandles(stock, tradingDate, affectedThirtyMinuteBuckets, finalizationCutoffTime);
        return new SyncStockResult(savedCount, skippedRecentCount, kisRequestCount);
    }

    private LocalDateTime resolveSyncStartTime(Long stockId, LocalDate tradingDate, LocalDateTime finalizationCutoffTime) {
        LocalDateTime marketOpenTime = LocalDateTime.of(finalizationCutoffTime.toLocalDate(), MARKET_OPEN_TIME);
        if (tradingDate.isBefore(LocalDate.now(KOREA_ZONE))) {
            return marketOpenTime;
        }

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

    private void upsertThirtyMinuteCandles(Stock stock,
                                           LocalDate tradingDate,
                                           Set<LocalDateTime> bucketStartTimes,
                                           LocalDateTime finalizationCutoffTime) {
        for (LocalDateTime bucketStartTime : bucketStartTimes) {
            LocalDateTime bucketEndTime = resolveThirtyMinuteBucketEndTime(bucketStartTime);
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

    private void addClosingAuctionSyntheticCandles(Stock stock,
                                                   LocalDate tradingDate,
                                                   LocalDateTime finalizationCutoffTime,
                                                   LocalDateTime syncStartTime,
                                                   List<KisMinuteCandleMetrics> candlesToSave) {
        LocalDateTime firstSyntheticTime = LocalDateTime.of(tradingDate, CLOSING_AUCTION_START_TIME);
        LocalDateTime lastSyntheticTime = LocalDateTime.of(tradingDate, CLOSING_AUCTION_LAST_SYNTHETIC_TIME);
        if (finalizationCutoffTime.isBefore(firstSyntheticTime) || syncStartTime.isAfter(lastSyntheticTime)) {
            return;
        }

        LocalDateTime baseTime = LocalDateTime.of(tradingDate, CLOSING_AUCTION_LAST_CONTINUOUS_TIME);
        KisMinuteCandleMetrics baseCandle = findCandle(candlesToSave, baseTime)
                .or(() -> stockCandleRepository.findByStockIdAndIntervalTypeAndCandleTime(
                        stock.getId(),
                        StockCandleIntervalType.MINUTE,
                        baseTime
                ).map(this::toMinuteCandleMetrics))
                .orElse(null);
        if (baseCandle == null) {
            return;
        }

        Map<LocalDateTime, KisMinuteCandleMetrics> candleMap = candlesToSave.stream()
                .collect(Collectors.toMap(KisMinuteCandleMetrics::candleTime, candle -> candle, (existing, replacement) -> replacement, LinkedHashMap::new));
        LocalDateTime cursor = firstSyntheticTime;
        while (!cursor.isAfter(lastSyntheticTime) && !cursor.isAfter(finalizationCutoffTime)) {
            if (!cursor.isBefore(syncStartTime)) {
                candleMap.putIfAbsent(cursor, createClosingAuctionSyntheticCandle(baseCandle, cursor));
            }
            cursor = cursor.plusMinutes(1);
        }

        candlesToSave.clear();
        candlesToSave.addAll(candleMap.values().stream()
                .sorted(Comparator.comparing(KisMinuteCandleMetrics::candleTime))
                .toList());
    }

    private Optional<KisMinuteCandleMetrics> findCandle(List<KisMinuteCandleMetrics> candles, LocalDateTime candleTime) {
        return candles.stream()
                .filter(candle -> candleTime.equals(candle.candleTime()))
                .findFirst();
    }

    private KisMinuteCandleMetrics toMinuteCandleMetrics(StockCandle candle) {
        return new KisMinuteCandleMetrics(
                candle.getCandleTime(),
                candle.getOpenPrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getClosePrice(),
                candle.getVolume(),
                candle.getTradeAmount()
        );
    }

    private KisMinuteCandleMetrics createClosingAuctionSyntheticCandle(KisMinuteCandleMetrics baseCandle, LocalDateTime candleTime) {
        BigDecimal price = baseCandle.closePrice();
        return new KisMinuteCandleMetrics(
                candleTime,
                price,
                price,
                price,
                price,
                0L,
                baseCandle.tradeAmount()
        );
    }

    private LocalDateTime resolveThirtyMinuteBucketEndTime(LocalDateTime bucketStartTime) {
        LocalDateTime marketCloseTime = LocalDateTime.of(bucketStartTime.toLocalDate(), MARKET_CLOSE_TIME);
        LocalTime bucketStartLocalTime = bucketStartTime.toLocalTime();

        if (bucketStartLocalTime.equals(LocalTime.of(15, 0))) {
            return LocalDateTime.of(bucketStartTime.toLocalDate(), CLOSING_AUCTION_LAST_SYNTHETIC_TIME);
        }
        if (bucketStartLocalTime.equals(MARKET_CLOSE_TIME)) {
            return marketCloseTime;
        }

        return bucketStartTime.plusMinutes(29).isAfter(marketCloseTime)
                ? marketCloseTime
                : bucketStartTime.plusMinutes(29);
    }

    private Set<LocalDateTime> resolveThirtyMinuteBucketStartTimes(LocalDate tradingDate) {
        Set<LocalDateTime> bucketStartTimes = new HashSet<>();
        LocalDateTime bucketStartTime = LocalDateTime.of(tradingDate, MARKET_OPEN_TIME);
        LocalDateTime marketCloseTime = LocalDateTime.of(tradingDate, MARKET_CLOSE_TIME);
        while (!bucketStartTime.isAfter(marketCloseTime)) {
            bucketStartTimes.add(bucketStartTime);
            bucketStartTime = bucketStartTime.plusMinutes(30);
        }
        return bucketStartTimes;
    }

    private record SyncStockResult(
            int savedCandleCount,
            int skippedRecentCandleCount,
            int kisRequestCount
    ) {
    }
}
