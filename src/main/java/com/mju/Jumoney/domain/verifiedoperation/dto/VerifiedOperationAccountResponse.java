package com.mju.Jumoney.domain.verifiedoperation.dto;

import com.mju.Jumoney.domain.verifiedoperation.enums.VerifiedOperationAccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VerifiedOperationAccountResponse(
        String accountCode,
        String accountName,
        VerifiedOperationAccountType type,
        List<VerifiedOperationConditionResponse> usedConditions,
        BigDecimal totalPurchaseAmount,
        BigDecimal totalEvaluationAmount,
        BigDecimal investmentProfitAmount,
        BigDecimal investmentProfitRate,
        BigDecimal totalAsset,
        BigDecimal totalProfitAmount,
        BigDecimal totalProfitRate,
        Integer holdingStockCount,
        LocalDateTime lastTradedAt,
        List<VerifiedOperationHoldingResponse> holdings
) {
}
