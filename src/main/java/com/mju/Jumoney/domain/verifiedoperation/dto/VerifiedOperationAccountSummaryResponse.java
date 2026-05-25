package com.mju.Jumoney.domain.verifiedoperation.dto;

import com.mju.Jumoney.domain.verifiedoperation.enums.VerifiedOperationAccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VerifiedOperationAccountSummaryResponse(
        List<AccountSummary> accounts
) {
    public record AccountSummary(
            String accountCode,
            String accountName,
            VerifiedOperationAccountType type,
            List<ConditionResponse> conditions,
            BigDecimal totalAsset,
            BigDecimal totalProfitAmount,
            BigDecimal totalProfitRate,
            Integer holdingStockCount,
            LocalDateTime lastTradedAt
    ) {
    }

    public record ConditionResponse(
            String code,
            String label
    ) {
    }
}
