package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

public record MasterCaseInitDto(
        MasterCode masterCode,
        String stockName,
        String sector,
        String investmentPeriod,
        String investmentResult,
        String title,
        String description
) {
}
