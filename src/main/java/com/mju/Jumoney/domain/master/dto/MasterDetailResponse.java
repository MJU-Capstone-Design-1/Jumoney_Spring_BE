package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.master.enums.MasterCode;

import java.util.List;

public record MasterDetailResponse(
        Long masterId,
        MasterCode masterCode,
        String masterName,
        List<String> tags,
        String quote,
        PhilosophyResponse philosophy,
        List<PrincipleResponse> principles
) {

    public record PhilosophyResponse(
            String title,
            String description
    ) {
    }

    public record PrincipleResponse(
            String title,
            String description,
            List<String> details
    ) {
    }
}
