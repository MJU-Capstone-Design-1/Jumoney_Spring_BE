package com.mju.Jumoney.global.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class BatchBaseDateResolver {

    private final ZoneId zoneId;
    private final LocalTime stockIndicatorTodayAvailableAfter;

    public BatchBaseDateResolver(
            @Value("${kis.batch.zone-id:Asia/Seoul}") String zoneId,
            @Value("${kis.batch.stock-indicator.today-available-after:15:40}") String stockIndicatorTodayAvailableAfter
    ) {
        this.zoneId = ZoneId.of(zoneId);
        this.stockIndicatorTodayAvailableAfter = LocalTime.parse(stockIndicatorTodayAvailableAfter);
    }

    public LocalDate resolveScheduledBaseDate() {
        return previousWeekday(today());
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

    private LocalDate previousWeekday(LocalDate date) {
        LocalDate previousDate = date.minusDays(1);
        while (isWeekend(previousDate)) {
            previousDate = previousDate.minusDays(1);
        }
        return previousDate;
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
