package com.mju.Jumoney.domain.mockinvestment.service;

import com.mju.Jumoney.domain.mockinvestment.exception.MockInvestmentErrorCode;
import com.mju.Jumoney.global.batch.MarketCalendarService;
import com.mju.Jumoney.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class MockInvestmentMarketService {

    private final ZoneId zoneId;
    private final LocalTime orderStartTime;
    private final LocalTime orderEndTime;
    private final MarketCalendarService marketCalendarService;

    public MockInvestmentMarketService(
            @Value("${mock-investment.market.zone-id:Asia/Seoul}") String zoneId,
            @Value("${mock-investment.market.order-start-time:09:00}") String orderStartTime,
            @Value("${mock-investment.market.order-end-time:15:20}") String orderEndTime,
            MarketCalendarService marketCalendarService
    ) {
        this.zoneId = ZoneId.of(zoneId);
        this.orderStartTime = LocalTime.parse(orderStartTime);
        this.orderEndTime = LocalTime.parse(orderEndTime);
        this.marketCalendarService = marketCalendarService;
    }

    public void validateOrderableNow() {
        LocalDate today = LocalDate.now(zoneId);
        LocalTime now = LocalTime.now(zoneId);

        boolean openDay = marketCalendarService.isOpenDay(today, zoneId);
        boolean withinOrderTime = !now.isBefore(orderStartTime) && !now.isAfter(orderEndTime);
        if (!openDay || !withinOrderTime) {
            throw new CustomException(MockInvestmentErrorCode.MARKET_CLOSED);
        }
    }
}
