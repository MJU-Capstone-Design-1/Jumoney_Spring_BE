package com.mju.Jumoney.domain.master.dto;

import jakarta.validation.constraints.NotNull;

public class MasterSelectionDTO {

    public record Request(
            @NotNull(message = "거장 ID는 필수입니다.")
            Long masterId
    ) {
    }

    public record Response(
            Long userId,
            Long masterId
    ) {
    }
}
