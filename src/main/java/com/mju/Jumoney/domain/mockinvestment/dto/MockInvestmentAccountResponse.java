package com.mju.Jumoney.domain.mockinvestment.dto;

import java.math.BigDecimal;

public record MockInvestmentAccountResponse(
        Long accountId,
        BigDecimal seedMoney,
        BigDecimal cashBalance,
        BigDecimal totalPurchaseAmount,
        BigDecimal totalAsset,
        BigDecimal totalProfitRate,
        boolean created
) {
}
