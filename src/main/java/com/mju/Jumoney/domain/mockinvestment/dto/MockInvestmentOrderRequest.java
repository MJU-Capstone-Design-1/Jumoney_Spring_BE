package com.mju.Jumoney.domain.mockinvestment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MockInvestmentOrderRequest(
        @Schema(description = "종목 코드", example = "005930")
        @NotNull
        String stockCode,
        @NotNull
        @Min(1)
        Integer quantity
) {
}
