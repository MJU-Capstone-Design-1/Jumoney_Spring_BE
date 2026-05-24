package com.mju.Jumoney.global.client.kis.dto.chart;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisPeriodChartOutput(
        @JsonProperty("stck_bsop_date") String businessDate,
        @JsonProperty("stck_oprc") String openPrice,
        @JsonProperty("stck_hgpr") String highPrice,
        @JsonProperty("stck_lwpr") String lowPrice,
        @JsonProperty("stck_clpr") String closePrice,
        @JsonProperty("acml_vol") String accumulatedVolume,
        @JsonProperty("acml_tr_pbmn") String accumulatedTradeAmount
) {
}
