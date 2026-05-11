package com.mju.Jumoney.domain.stock.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class StockIndicatorBatchAvailability {

    private final ZoneId zoneId;
    private final LocalTime todayAvailableAfter;

    public StockIndicatorBatchAvailability(
            @Value("${kis.batch.stock-indicator.zone-id:Asia/Seoul}") String zoneId,
            @Value("${kis.batch.stock-indicator.today-available-after:15:40}") String todayAvailableAfter
    ) {
        this.zoneId = ZoneId.of(zoneId);
        this.todayAvailableAfter = LocalTime.parse(todayAvailableAfter);
    }

    public LocalDate latestAvailableBaseDate() {
        LocalDate today = today();
        if (!isWeekend(today) && isTodayAvailable()) {
            return today;
        }
        return previousWeekday(today);
    }

    public void validateAvailable(LocalDate baseDate) {
        if (baseDate.isEqual(today()) && !isTodayAvailable()) {
            throw new IllegalStateException("오늘 기준 종목 지표 배치는 " + todayAvailableAfter
                    + " 이후에 실행할 수 있습니다. KIS 투자자매매동향 일별 API(FHPTJ04160001)가 장중에는 "
                    + "TIME LIMIT 00:00 ~ " + todayAvailableAfter + " 오류를 반환합니다. "
                    + "baseDate를 생략해 최신 조회 가능 기준일로 실행하거나, 이전 거래일을 명시하세요. baseDate=" + baseDate);
        }
    }

    private boolean isTodayAvailable() {
        return !LocalTime.now(zoneId).isBefore(todayAvailableAfter);
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
