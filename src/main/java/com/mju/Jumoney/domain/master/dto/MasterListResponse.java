package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

import java.util.List;

public record MasterListResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName,
        List<String> tags,
        boolean isSelected
) {
}
