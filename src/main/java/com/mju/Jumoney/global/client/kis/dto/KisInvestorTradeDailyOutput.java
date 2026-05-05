package com.mju.Jumoney.global.client.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// FHPTJ04160001(종목별 투자자매매동향 일별)의 output2 한 행입니다.
public record KisInvestorTradeDailyOutput(
        @JsonProperty("stck_bsop_date") String businessDate,
        @JsonProperty("orgn_ntby_qty") String institutionNetBuyQuantity
) {
}
