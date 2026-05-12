package com.mju.Jumoney.global.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mju.Jumoney.global.client.kis.core.KisApiClient;
import com.mju.Jumoney.global.client.kis.dto.market.KisDomesticHolidayOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketCalendarService {

    private static final String MARKET_CODE_KRX = "KRX";
    private static final String REDIS_KEY_PREFIX = "market:calendar:";
    private static final DateTimeFormatter REDIS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final KisApiClient kisApiClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public LocalDate resolvePreviousOpenDay(LocalDate today, int lookbackDays, ZoneId zoneId) {
        LocalDate previousDate = today.minusDays(1);
        if (!findCalendarDay(previousDate).isPresent()) {
            LocalDate queryBaseDate = today.minusDays(lookbackDays);
            syncCalendarFromKis(queryBaseDate, zoneId);
        }

        return findPreviousOpenDayInRedis(today, lookbackDays)
                .orElseThrow(() -> new IllegalStateException("Redis 캘린더에서 직전 개장일을 찾을 수 없습니다. queryBaseDate="
                        + today.minusDays(lookbackDays) + ", today=" + today));
    }

    private Optional<LocalDate> findPreviousOpenDayInRedis(LocalDate today, int lookbackDays) {
        LocalDate from = today.minusDays(lookbackDays);
        return from.datesUntil(today)
                .sorted(Comparator.reverseOrder())
                .map(this::findCalendarDay)
                .flatMap(Optional::stream)
                .filter(MarketCalendarDay::openDay)
                .map(MarketCalendarDay::date)
                .findFirst();
    }

    private Optional<MarketCalendarDay> findCalendarDay(LocalDate date) {
        String json = stringRedisTemplate.opsForValue().get(redisKey(date));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, MarketCalendarDay.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 캘린더 JSON 파싱 실패: key=" + redisKey(date), e);
        }
    }

    private void syncCalendarFromKis(LocalDate queryBaseDate, ZoneId zoneId) {
        List<KisDomesticHolidayOutput> outputs = kisApiClient.getDomesticHolidays(queryBaseDate);
        OffsetDateTime fetchedAt = OffsetDateTime.now(zoneId);
        for (KisDomesticHolidayOutput output : outputs) {
            saveCalendarDay(MarketCalendarDay.from(output, fetchedAt));
        }
    }

    private void saveCalendarDay(MarketCalendarDay day) {
        try {
            stringRedisTemplate.opsForValue().set(redisKey(day.date()), objectMapper.writeValueAsString(day));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Redis 캘린더 JSON 직렬화 실패: date=" + day.date(), e);
        }
    }

    private String redisKey(LocalDate date) {
        return REDIS_KEY_PREFIX + MARKET_CODE_KRX + ":" + REDIS_DATE_FORMATTER.format(date);
    }
}
