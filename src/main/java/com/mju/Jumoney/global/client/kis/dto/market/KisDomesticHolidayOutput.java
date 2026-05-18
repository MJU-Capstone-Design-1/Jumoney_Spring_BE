package com.mju.Jumoney.global.client.kis.dto.market;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public record KisDomesticHolidayOutput(
        @JsonProperty("bass_dt") String baseDate,
        @JsonProperty("wday_dvsn_cd") String weekdayDivisionCode,
        @JsonProperty("bzdy_yn") String businessDayYn,
        @JsonProperty("tr_day_yn") String tradingDayYn,
        @JsonProperty("opnd_yn") String openDayYn,
        @JsonProperty("sttl_day_yn") String settlementDayYn
) {

    private static final DateTimeFormatter KIS_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String YES = "Y";

    public LocalDate toLocalDate() {
        return LocalDate.parse(baseDate, KIS_DATE_FORMATTER);
    }

    public boolean isOpenDay() {
        return YES.equals(openDayYn);
    }
}
