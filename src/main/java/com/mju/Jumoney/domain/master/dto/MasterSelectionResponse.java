package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

public record MasterSelectionResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName
) {
}
