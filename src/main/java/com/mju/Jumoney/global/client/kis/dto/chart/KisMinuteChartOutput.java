package com.mju.Jumoney.global.client.kis.dto.chart;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KisMinuteChartOutput(
        @JsonProperty("stck_bsop_date") String businessDate,
        @JsonProperty("stck_cntg_hour") String executionTime,
        @JsonProperty("stck_prpr") String currentPrice,
        @JsonProperty("stck_oprc") String openPrice,
        @JsonProperty("stck_hgpr") String highPrice,
        @JsonProperty("stck_lwpr") String lowPrice,
        @JsonProperty("cntg_vol") String executionVolume,
        @JsonProperty("acml_tr_pbmn") String accumulatedTradeAmount
) {
}
