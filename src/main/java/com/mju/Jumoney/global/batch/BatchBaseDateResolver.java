package com.mju.Jumoney.global.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class BatchBaseDateResolver {

    private final ZoneId zoneId;
    private final LocalTime stockIndicatorTodayAvailableAfter;
    private final int openingDayLookbackDays;
    private final MarketCalendarService marketCalendarService;

    public BatchBaseDateResolver(
            @Value("${kis.batch.zone-id:Asia/Seoul}") String zoneId,
            @Value("${kis.batch.stock-indicator.today-available-after:15:40}") String stockIndicatorTodayAvailableAfter,
            @Value("${kis.batch.opening-day-lookback-days:14}") int openingDayLookbackDays,
            MarketCalendarService marketCalendarService
    ) {
        this.zoneId = ZoneId.of(zoneId);
        this.stockIndicatorTodayAvailableAfter = LocalTime.parse(stockIndicatorTodayAvailableAfter);
        this.openingDayLookbackDays = openingDayLookbackDays;
        this.marketCalendarService = marketCalendarService;
    }

    public LocalDate resolveScheduledBaseDate() {
        LocalDate today = today();
        return marketCalendarService.resolvePreviousOpenDay(today, openingDayLookbackDays, zoneId);
    }

    public void validateStockIndicatorManualBaseDate(LocalDate baseDate) {
        if (baseDate.isEqual(today()) && !isStockIndicatorTodayAvailable()) {
            throw new IllegalStateException("오늘 기준 종목 지표 배치는 " + stockIndicatorTodayAvailableAfter
                    + " 이후에 실행할 수 있습니다. KIS 투자자매매동향 일별 API(FHPTJ04160001)가 장중에는 "
                    + "TIME LIMIT 00:00 ~ " + stockIndicatorTodayAvailableAfter + " 오류를 반환합니다. "
                    + "이전 거래일을 명시하거나, 장 마감 후 다시 실행하세요. baseDate=" + baseDate);
        }
    }

    private boolean isStockIndicatorTodayAvailable() {
        return !LocalTime.now(zoneId).isBefore(stockIndicatorTodayAvailableAfter);
    }

    private LocalDate today() {
        return LocalDate.now(zoneId);
    }

}
