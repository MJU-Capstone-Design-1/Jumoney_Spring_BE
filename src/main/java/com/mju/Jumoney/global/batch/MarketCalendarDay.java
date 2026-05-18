package com.mju.Jumoney.global.batch;

import com.mju.Jumoney.global.client.kis.dto.market.KisDomesticHolidayOutput;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record MarketCalendarDay(
        LocalDate date,
        boolean businessDay,
        boolean tradingDay,
        boolean openDay,
        boolean settlementDay,
        String source,
        OffsetDateTime fetchedAt
) {

    private static final String YES = "Y";
    private static final String SOURCE_KIS = "KIS";

    public static MarketCalendarDay from(KisDomesticHolidayOutput output, OffsetDateTime fetchedAt) {
        return new MarketCalendarDay(
                output.toLocalDate(),
                YES.equals(output.businessDayYn()),
                YES.equals(output.tradingDayYn()),
                YES.equals(output.openDayYn()),
                YES.equals(output.settlementDayYn()),
                SOURCE_KIS,
                fetchedAt
        );
    }
}
