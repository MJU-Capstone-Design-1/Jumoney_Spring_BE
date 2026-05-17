package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

public record MasterInitDto(
        MasterCode masterCode,
        String masterName,
        String quote,
        String imageFileName,
        String returnRate,
        String portfolioBasePeriod,
        String philosophyTitle,
        String philosophyDescription,
        String recommendationDescription,
        int displayOrder
) {
}
