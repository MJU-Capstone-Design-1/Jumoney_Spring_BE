package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;

public record MasterOptionInitDto(
        MasterCode masterCode,
        String content,
        String description,
        MasterOptionLogicCode logicCode,
        int displayOrder
) {
}
