package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;

public record MasterOptionInitDto(
        String masterName,
        String content,
        String description,
        MasterOptionLogicCode logicCode,
        int displayOrder
) {
}
