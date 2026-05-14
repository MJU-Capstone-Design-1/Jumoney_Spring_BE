package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

public record MasterInitDto(
        MasterCode masterCode,
        String masterName,
        String recommendationDescription,
        int displayOrder
) {
}
