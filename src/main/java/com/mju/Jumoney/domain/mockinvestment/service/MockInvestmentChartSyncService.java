package com.mju.Jumoney.domain.mockinvestment.service;

import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentChartCandleSyncResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentChartCandleSyncStatusResponse;
import com.mju.Jumoney.domain.mockinvestment.enums.MockInvestmentChartPeriod;
import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.domain.stock.domain.StockCandle;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncFailureResponse;
import com.mju.Jumoney.domain.stock.dto.MinuteCandleSyncResponse;
import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import com.mju.Jumoney.domain.stock.exception.StockErrorCode;
import com.mju.Jumoney.domain.stock.repository.StockCandleRepository;
import com.mju.Jumoney.domain.stock.repository.StockRepository;
import com.mju.Jumoney.domain.stock.service.StockMinuteCandleSyncService;
import com.mju.Jumoney.global.batch.MarketCalendarService;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.chart.KisPeriodCandleMetrics;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MockInvestmentChartSyncService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final LocalTime MARKET_OPEN_TIME = LocalTime.of(9, 0);
    private static final LocalTime MARKET_CLOSE_TIME = LocalTime.of(15, 30);
    private static final int THIRTY_MINUTE_BUCKET_COUNT_PER_OPEN_DAY = 14;
    private static final int DAY_CANDLE_CHUNK_DAYS = 90;
    private static final int WEEK_CANDLE_CHUNK_DAYS = 365;
    private static final int ONE_WEEK_OPEN_DAY_COUNT = 5;

    private final StockMinuteCandleSyncService stockMinuteCandleSyncService;
    private final KisApiClient kisApiClient;
    private final StockRepository stockRepository;
    private final StockCandleRepository stockCandleRepository;
    private final MarketCalendarService marketCalendarService;

    @Value("${kis.batch.opening-day-lookback-days:14}")
    private int openingDayLookbackDays;

    public MockInvestmentChartCandleSyncResponse syncChartCandles(String stockCode,
                                                                  MockInvestmentChartPeriod period,
                                                                  LocalDate date) {
        String normalizedStockCode = normalizeStockCode(stockCode);
        LocalDate targetDate = resolveTargetDate(date);
        Set<MockInvestmentChartPeriod> targetPeriods = resolveTargetPeriods(period);
        LocalDateTime requestedAt = LocalDateTime.now(KST_ZONE_ID).truncatedTo(ChronoUnit.SECONDS);

        List<MockInvestmentChartCandleSyncResponse.SourceSync> sourceSyncs = new ArrayList<>();
        if (targetPeriods.contains(MockInvestmentChartPeriod.ONE_WEEK)) {
            List<LocalDate> oneWeekOpenDays = resolveRecentOpenDays(targetDate, ONE_WEEK_OPEN_DAY_COUNT);
            sourceSyncs.add(syncMinuteSource(normalizedStockCode, oneWeekOpenDays.get(0), targetDate));
        } else if (targetPeriods.contains(MockInvestmentChartPeriod.ONE_DAY)) {
            sourceSyncs.add(syncMinuteSource(normalizedStockCode, targetDate, targetDate));
        }

        if (targetPeriods.contains(MockInvestmentChartPeriod.ONE_YEAR)) {
            sourceSyncs.add(syncKisPeriodSource(normalizedStockCode, StockCandleIntervalType.DAY, targetDate.minusYears(1), targetDate));
        } else if (targetPeriods.contains(MockInvestmentChartPeriod.THREE_MONTHS)) {
            sourceSyncs.add(syncKisPeriodSource(normalizedStockCode, StockCandleIntervalType.DAY, targetDate.minusMonths(3), targetDate));
        }

        if (targetPeriods.contains(MockInvestmentChartPeriod.FIVE_YEARS)) {
            sourceSyncs.add(syncKisPeriodSource(normalizedStockCode, StockCandleIntervalType.WEEK, targetDate.minusYears(5), targetDate));
        }

        return new MockInvestmentChartCandleSyncResponse(
                normalizedStockCode,
                targetDate,
                requestedAt,
                targetPeriods.stream().map(Enum::name).toList(),
                sourceSyncs
        );
    }

    public MockInvestmentChartCandleSyncResponse syncChartCandlesInRange(String stockCode,
                                                                         MockInvestmentChartPeriod period,
                                                                         LocalDate fromDate,
                                                                         LocalDate toDate) {
        if (period == null) {
            throw new IllegalArgumentException("차트 기간이 필요합니다.");
        }
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("동기화 시작일과 종료일이 필요합니다.");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("동기화 시작일은 종료일보다 늦을 수 없습니다. fromDate=" + fromDate + ", toDate=" + toDate);
        }

        String normalizedStockCode = normalizeStockCode(stockCode);
        LocalDateTime requestedAt = LocalDateTime.now(KST_ZONE_ID).truncatedTo(ChronoUnit.SECONDS);
        MockInvestmentChartCandleSyncResponse.SourceSync sourceSync = switch (period) {
            case ONE_DAY, ONE_WEEK -> syncMinuteSource(normalizedStockCode, fromDate, toDate);
            case THREE_MONTHS, ONE_YEAR ->
                    syncKisPeriodSource(normalizedStockCode, StockCandleIntervalType.DAY, fromDate, toDate);
            case FIVE_YEARS -> syncKisPeriodSource(normalizedStockCode, StockCandleIntervalType.WEEK, fromDate, toDate);
        };

        return new MockInvestmentChartCandleSyncResponse(
                normalizedStockCode,
                toDate,
                requestedAt,
                List.of(period.name()),
                List.of(sourceSync)
        );
    }

    public List<MockInvestmentChartCandleSyncResponse.SourceSync> syncLatestFinalPeriodCandles(String stockCode,
                                                                                               LocalDate finalDailyDate) {
        String normalizedStockCode = normalizeStockCode(stockCode);
        List<MockInvestmentChartCandleSyncResponse.SourceSync> sourceSyncs = new ArrayList<>();
        sourceSyncs.add(syncKisPeriodSource(normalizedStockCode, StockCandleIntervalType.DAY, finalDailyDate, finalDailyDate));

        if (isLastOpenDayOfWeek(finalDailyDate)) {
            sourceSyncs.add(syncKisPeriodSource(
                    normalizedStockCode,
                    StockCandleIntervalType.WEEK,
                    finalDailyDate.minusDays(10),
                    finalDailyDate
            ));
        }

        return sourceSyncs;
    }

    public MockInvestmentChartCandleSyncStatusResponse getChartCandleSyncStatus(String stockCode,
                                                                                MockInvestmentChartPeriod period,
                                                                                LocalDate date) {
        Stock stock = stockRepository.findByStockCode(stockCode)
                .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));
        LocalDate targetDate = resolveTargetDate(date);
        Set<MockInvestmentChartPeriod> targetPeriods = resolveTargetPeriods(period);

        List<MockInvestmentChartCandleSyncStatusResponse.PeriodStatus> statuses = targetPeriods.stream()
                .map(targetPeriod -> getPeriodStatus(stock, targetPeriod, targetDate))
                .toList();

        return new MockInvestmentChartCandleSyncStatusResponse(
                stock.getStockCode(),
                stock.getName(),
                targetDate,
                statuses
        );
    }

    private MockInvestmentChartCandleSyncResponse.SourceSync syncMinuteSource(String stockCode,
                                                                              LocalDate fromDate,
                                                                              LocalDate toDate) {
        List<MinuteCandleSyncResponse> responses = resolveOpenDays(fromDate, toDate).stream()
                .map(openDay -> stockMinuteCandleSyncService.syncMinuteCandles(stockCode, openDay))
                .toList();

        return new MockInvestmentChartCandleSyncResponse.SourceSync(
                "KIS_MINUTE",
                StockCandleIntervalType.MINUTE.name(),
                fromDate,
                toDate,
                responses.stream().mapToInt(MinuteCandleSyncResponse::targetStockCount).max().orElse(0),
                responses.stream().mapToInt(MinuteCandleSyncResponse::kisRequestCount).sum(),
                responses.stream().mapToInt(MinuteCandleSyncResponse::successCount).sum(),
                responses.stream().mapToInt(MinuteCandleSyncResponse::failureCount).sum(),
                responses.stream().mapToInt(MinuteCandleSyncResponse::savedCandleCount).sum(),
                responses.stream().mapToInt(MinuteCandleSyncResponse::skippedRecentCandleCount).sum(),
                responses.stream()
                        .flatMap(response -> response.failures().stream())
                        .map(this::toFailure)
                        .toList()
        );
    }

    private MockInvestmentChartCandleSyncResponse.SourceSync syncKisPeriodSource(String stockCode,
                                                                                 StockCandleIntervalType intervalType,
                                                                                 LocalDate fromDate,
                                                                                 LocalDate toDate) {
        List<Stock> targetStocks = resolveTargetStocks(stockCode);
        List<MockInvestmentChartCandleSyncResponse.Failure> failures = new ArrayList<>();
        int successCount = 0;
        int savedCandleCount = 0;
        int kisRequestCount = 0;

        for (Stock stock : targetStocks) {
            try {
                PeriodSourceSyncResult result = syncKisPeriodSourceForStock(stock, intervalType, fromDate, toDate);
                savedCandleCount += result.savedCandleCount();
                kisRequestCount += result.kisRequestCount();
                successCount++;
            } catch (Exception e) {
                failures.add(new MockInvestmentChartCandleSyncResponse.Failure(
                        stock.getStockCode(),
                        stock.getName(),
                        e.getMessage()
                ));
            }
        }

        return new MockInvestmentChartCandleSyncResponse.SourceSync(
                "KIS_PERIOD",
                intervalType.name(),
                fromDate,
                toDate,
                targetStocks.size(),
                kisRequestCount,
                successCount,
                failures.size(),
                savedCandleCount,
                0,
                failures
        );
    }

    private PeriodSourceSyncResult syncKisPeriodSourceForStock(Stock stock,
                                                               StockCandleIntervalType intervalType,
                                                               LocalDate fromDate,
                                                               LocalDate toDate) {
        PeriodCandleLoadResult loadResult = loadPeriodCandles(stock.getStockCode(), intervalType, fromDate, toDate);
        List<KisPeriodCandleMetrics> candles = loadResult.candles();
        if (candles.isEmpty()) {
            return new PeriodSourceSyncResult(0, loadResult.kisRequestCount());
        }

        Map<LocalDateTime, StockCandle> existingCandleMap = stockCandleRepository.findByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                        stock.getId(),
                        intervalType,
                        candles.get(0).candleTime(),
                        candles.get(candles.size() - 1).candleTime()
                ).stream()
                .collect(Collectors.toMap(StockCandle::getCandleTime, candle -> candle));

        for (KisPeriodCandleMetrics candle : candles) {
            upsertFinalPeriodCandle(stock, intervalType, candle, existingCandleMap.get(candle.candleTime()));
        }
        return new PeriodSourceSyncResult(candles.size(), loadResult.kisRequestCount());
    }

    private PeriodCandleLoadResult loadPeriodCandles(String stockCode,
                                                     StockCandleIntervalType intervalType,
                                                     LocalDate fromDate,
                                                     LocalDate toDate) {
        String periodCode = toKisPeriodCode(intervalType);
        int chunkDays = periodChunkDays(intervalType);
        Map<LocalDateTime, KisPeriodCandleMetrics> candleMap = new TreeMap<>();
        int kisRequestCount = 0;

        LocalDate chunkFrom = fromDate;
        while (!chunkFrom.isAfter(toDate)) {
            LocalDate chunkTo = chunkFrom.plusDays(chunkDays - 1);
            if (chunkTo.isAfter(toDate)) {
                chunkTo = toDate;
            }

            kisRequestCount++;
            kisApiClient.getPeriodCandles(stockCode, chunkFrom, chunkTo, periodCode).stream()
                    .filter(this::isValidKisPeriodCandle)
                    .forEach(candle -> candleMap.put(candle.candleTime(), candle));

            chunkFrom = chunkTo.plusDays(1);
        }

        return new PeriodCandleLoadResult(
                candleMap.values().stream()
                        .sorted(Comparator.comparing(KisPeriodCandleMetrics::candleTime))
                        .toList(),
                kisRequestCount
        );
    }

    private int periodChunkDays(StockCandleIntervalType intervalType) {
        return switch (intervalType) {
            case DAY -> DAY_CANDLE_CHUNK_DAYS;
            case WEEK -> WEEK_CANDLE_CHUNK_DAYS;
            default -> throw new IllegalArgumentException("KIS 기간봉은 DAY 또는 WEEK만 지원합니다. intervalType=" + intervalType);
        };
    }

    private void upsertFinalPeriodCandle(Stock stock,
                                         StockCandleIntervalType intervalType,
                                         KisPeriodCandleMetrics candle,
                                         StockCandle existingCandle) {
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

        stockCandleRepository.save(StockCandle.createFinal(
                stock,
                intervalType,
                candle.candleTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume(),
                candle.tradeAmount()
        ));
    }

    private MockInvestmentChartCandleSyncResponse.Failure toFailure(MinuteCandleSyncFailureResponse failure) {
        return new MockInvestmentChartCandleSyncResponse.Failure(
                failure.stockCode(),
                failure.stockName(),
                failure.message()
        );
    }

    private MockInvestmentChartCandleSyncStatusResponse.PeriodStatus getPeriodStatus(Stock stock,
                                                                                     MockInvestmentChartPeriod period,
                                                                                     LocalDate targetDate) {
        ChartStatusRange range = resolveStatusRange(period, targetDate);
        long candleCount = stockCandleRepository.countByStockIdAndIntervalTypeAndCandleTimeBetween(
                stock.getId(),
                range.intervalType(),
                range.startTime(),
                range.endTime()
        );
        LocalDateTime firstCandleTime = stockCandleRepository.findFirstByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeAsc(
                        stock.getId(),
                        range.intervalType(),
                        range.startTime(),
                        range.endTime()
                )
                .map(StockCandle::getCandleTime)
                .orElse(null);
        LocalDateTime lastCandleTime = stockCandleRepository.findFirstByStockIdAndIntervalTypeAndCandleTimeBetweenOrderByCandleTimeDesc(
                        stock.getId(),
                        range.intervalType(),
                        range.startTime(),
                        range.endTime()
                )
                .map(StockCandle::getCandleTime)
                .orElse(null);

        boolean hasAnyCandle = candleCount > 0;
        boolean hasExpectedCandleCount = range.expectedCandleCount() == null || candleCount >= range.expectedCandleCount();
        boolean coversExpectedRange = hasAnyCandle
                && !firstCandleTime.isAfter(range.startTime().plusDays(range.startToleranceDays()))
                && !lastCandleTime.isBefore(range.endTime());
        boolean complete = hasAnyCandle && hasExpectedCandleCount && coversExpectedRange;

        return new MockInvestmentChartCandleSyncStatusResponse.PeriodStatus(
                period.name(),
                range.intervalType().name(),
                range.startTime(),
                range.endTime(),
                range.expectedCandleCount(),
                candleCount,
                firstCandleTime,
                lastCandleTime,
                hasAnyCandle,
                hasExpectedCandleCount,
                coversExpectedRange,
                complete,
                buildStatusMessage(complete, range.expectedCandleCount())
        );
    }

    private Set<MockInvestmentChartPeriod> resolveTargetPeriods(MockInvestmentChartPeriod period) {
        if (period != null) {
            return EnumSet.of(period);
        }
        return EnumSet.allOf(MockInvestmentChartPeriod.class);
    }

    private List<Stock> resolveTargetStocks(String stockCode) {
        if (stockCode != null) {
            Stock stock = stockRepository.findByStockCode(stockCode)
                    .orElseThrow(() -> new CustomException(StockErrorCode.STOCK_NOT_FOUND));
            return List.of(stock);
        }
        return stockRepository.findAll();
    }

    private LocalDate resolveTargetDate(LocalDate requestedDate) {
        if (requestedDate != null) {
            return requestedDate;
        }

        LocalDate today = LocalDate.now(KST_ZONE_ID);
        if (marketCalendarService.isOpenDay(today, KST_ZONE_ID)) {
            return today;
        }
        return marketCalendarService.resolvePreviousOpenDay(today, openingDayLookbackDays, KST_ZONE_ID);
    }

    private List<LocalDate> resolveOpenDays(LocalDate fromDate, LocalDate toDate) {
        return fromDate.datesUntil(toDate.plusDays(1))
                .filter(date -> marketCalendarService.isOpenDay(date, KST_ZONE_ID))
                .toList();
    }

    private List<LocalDate> resolveRecentOpenDays(LocalDate targetDate, int openDayCount) {
        List<LocalDate> openDays = new ArrayList<>();
        LocalDate currentDate = targetDate;
        int lookbackLimit = Math.max(openingDayLookbackDays, openDayCount * 4);
        for (int checkedDays = 0; checkedDays <= lookbackLimit && openDays.size() < openDayCount; checkedDays++) {
            if (marketCalendarService.isOpenDay(currentDate, KST_ZONE_ID)) {
                openDays.add(currentDate);
            }
            currentDate = currentDate.minusDays(1);
        }
        if (openDays.size() < openDayCount) {
            throw new IllegalStateException("최근 영업일을 충분히 찾을 수 없습니다. targetDate=" + targetDate + ", openDayCount=" + openDayCount);
        }
        Collections.reverse(openDays);
        return openDays;
    }

    private boolean isLastOpenDayOfWeek(LocalDate date) {
        LocalDate weekEnd = date.plusDays(6L - date.getDayOfWeek().getValue());
        return date.plusDays(1).datesUntil(weekEnd.plusDays(1))
                .noneMatch(nextDate -> marketCalendarService.isOpenDay(nextDate, KST_ZONE_ID));
    }

    private ChartStatusRange resolveStatusRange(MockInvestmentChartPeriod period, LocalDate targetDate) {
        return switch (period) {
            case ONE_DAY -> {
                LocalDateTime expectedEndTime = resolveExpectedMinuteEndTime(targetDate);
                yield new ChartStatusRange(
                        StockCandleIntervalType.MINUTE,
                        LocalDateTime.of(targetDate, MARKET_OPEN_TIME),
                        expectedEndTime,
                        ChronoUnit.MINUTES.between(MARKET_OPEN_TIME, expectedEndTime.toLocalTime()) + 1,
                        0
                );
            }
            case ONE_WEEK -> {
                List<LocalDate> openDays = resolveRecentOpenDays(targetDate, ONE_WEEK_OPEN_DAY_COUNT);
                LocalDate expectedStartDate = openDays.get(0);
                yield new ChartStatusRange(
                        StockCandleIntervalType.THIRTY_MINUTE,
                        LocalDateTime.of(expectedStartDate, MARKET_OPEN_TIME),
                        resolveExpectedThirtyMinuteEndTime(targetDate),
                        calculateExpectedThirtyMinuteCount(openDays, targetDate),
                        0
                );
            }
            case THREE_MONTHS -> new ChartStatusRange(
                    StockCandleIntervalType.DAY,
                    targetDate.minusMonths(3).atStartOfDay(),
                    targetDate.atStartOfDay(),
                    null,
                    7
            );
            case ONE_YEAR -> new ChartStatusRange(
                    StockCandleIntervalType.DAY,
                    targetDate.minusYears(1).atStartOfDay(),
                    targetDate.atStartOfDay(),
                    null,
                    7
            );
            case FIVE_YEARS -> new ChartStatusRange(
                    StockCandleIntervalType.WEEK,
                    targetDate.minusYears(5).atStartOfDay(),
                    targetDate.atStartOfDay(),
                    null,
                    14
            );
        };
    }

    private String normalizeStockCode(String stockCode) {
        return StringUtils.hasText(stockCode) ? stockCode.trim() : null;
    }

    private String toKisPeriodCode(StockCandleIntervalType intervalType) {
        return switch (intervalType) {
            case DAY -> "D";
            case WEEK -> "W";
            default -> throw new IllegalArgumentException("KIS 기간봉은 DAY 또는 WEEK만 지원합니다. intervalType=" + intervalType);
        };
    }

    private boolean isValidKisPeriodCandle(KisPeriodCandleMetrics candle) {
        return candle.candleTime() != null
                && candle.openPrice() != null
                && candle.highPrice() != null
                && candle.lowPrice() != null
                && candle.closePrice() != null
                && candle.volume() != null;
    }

    private String buildStatusMessage(boolean complete, Long expectedCandleCount) {
        if (complete) {
            return "차트 조회에 필요한 DB 확정 캔들 범위를 충족합니다.";
        }
        if (expectedCandleCount == null) {
            return "DB 확정 캔들의 시작/종료 범위가 부족합니다. /api/local/kis/chart/sync로 보정하세요.";
        }
        return "DB 확정 캔들 개수 또는 범위가 부족합니다. /api/local/kis/chart/sync로 보정하세요.";
    }

    private LocalDateTime resolveExpectedMinuteEndTime(LocalDate targetDate) {
        if (!targetDate.equals(LocalDate.now(KST_ZONE_ID))) {
            return LocalDateTime.of(targetDate, MARKET_CLOSE_TIME);
        }

        LocalDateTime bufferedTime = LocalDateTime.now(KST_ZONE_ID).truncatedTo(ChronoUnit.MINUTES).minusMinutes(2);
        LocalDateTime marketOpen = LocalDateTime.of(targetDate, MARKET_OPEN_TIME);
        LocalDateTime marketClose = LocalDateTime.of(targetDate, MARKET_CLOSE_TIME);
        if (bufferedTime.isBefore(marketOpen)) {
            return marketOpen;
        }
        if (bufferedTime.isAfter(marketClose)) {
            return marketClose;
        }
        return bufferedTime;
    }

    private LocalDateTime resolveExpectedThirtyMinuteEndTime(LocalDate targetDate) {
        if (!targetDate.equals(LocalDate.now(KST_ZONE_ID))) {
            return LocalDateTime.of(targetDate, MARKET_CLOSE_TIME);
        }

        LocalDateTime minuteEndTime = resolveExpectedMinuteEndTime(targetDate);
        LocalDateTime marketOpen = LocalDateTime.of(targetDate, MARKET_OPEN_TIME);
        if (minuteEndTime.isBefore(marketOpen.plusMinutes(29))) {
            return marketOpen;
        }

        int flooredMinute = minuteEndTime.getMinute() >= 30 ? 30 : 0;
        LocalDateTime currentBucketStart = minuteEndTime.withMinute(flooredMinute).withSecond(0).withNano(0);
        LocalDateTime currentBucketEnd = currentBucketStart.plusMinutes(29);
        if (!currentBucketEnd.isAfter(minuteEndTime)) {
            return currentBucketStart;
        }
        return currentBucketStart.minusMinutes(30);
    }

    private long calculateExpectedThirtyMinuteCount(List<LocalDate> openDays, LocalDate targetDate) {
        if (openDays.isEmpty()) {
            return 0;
        }
        long completedOpenDaysBeforeTarget = openDays.stream()
                .filter(openDay -> openDay.isBefore(targetDate))
                .count();
        long targetDateBucketCount = openDays.contains(targetDate)
                ? ChronoUnit.MINUTES.between(MARKET_OPEN_TIME, resolveExpectedThirtyMinuteEndTime(targetDate).toLocalTime()) / 30 + 1
                : 0;
        return completedOpenDaysBeforeTarget * THIRTY_MINUTE_BUCKET_COUNT_PER_OPEN_DAY + targetDateBucketCount;
    }

    private record ChartStatusRange(
            StockCandleIntervalType intervalType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Long expectedCandleCount,
            int startToleranceDays
    ) {
    }

    private record PeriodCandleLoadResult(
            List<KisPeriodCandleMetrics> candles,
            int kisRequestCount
    ) {
    }

    private record PeriodSourceSyncResult(
            int savedCandleCount,
            int kisRequestCount
    ) {
    }
}
