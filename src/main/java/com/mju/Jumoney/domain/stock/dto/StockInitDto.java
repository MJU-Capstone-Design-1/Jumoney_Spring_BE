package com.mju.Jumoney.domain.stock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StockInitDto(
        String ticker,
        String name,
        String sectorName,
        @JsonProperty("isLeader") boolean isLeader,
        String marketCode,
        String description
) {
}
