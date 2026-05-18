package com.mju.Jumoney.global.client.kis.smoke.dto;

import java.time.LocalDate;
import java.util.List;

public record KisSmokeResponse(
        String stockCode,
        LocalDate baseDate,
        LocalDate dividendFrom,
        LocalDate dividendTo,
        int totalCount,
        int successCount,
        int failureCount,
        List<KisSmokeApiResult> results
) {
}
