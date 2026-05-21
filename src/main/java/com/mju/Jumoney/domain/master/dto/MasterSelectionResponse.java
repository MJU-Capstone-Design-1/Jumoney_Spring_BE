package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterSelectionStatus;

public record MasterSelectionResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName,
        MasterSelectionStatus selectionStatus
) {
}
