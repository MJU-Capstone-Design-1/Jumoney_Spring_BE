package com.mju.Jumoney.global.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// FHPST04760000(국내주식 신용잔고 일별추이)의 output 한 행입니다.
public record KisCreditBalanceOutput(
        @JsonProperty("deal_date") String tradeDate,
        @JsonProperty("stlm_date") String settlementDate,
        @JsonProperty("whol_loan_rmnd_rate") String totalLoanBalanceRate
) {
}
