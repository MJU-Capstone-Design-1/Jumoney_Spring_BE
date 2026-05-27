package com.mju.Jumoney.domain.home.dto;

import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentSummaryResponse.TopHolding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HomeMockInvestmentRankingsResponse(
        RankingSection overall,
        List<RankingSection> masters
) {

    public record RankingSection(
            String scope,
            Long masterId,
            String masterCode,
            String masterName,
            LocalDate rankingDate,
            List<RankingUser> users
    ) {
    }

    public record RankingUser(
            int rank,
            Long userId,
            Long masterId,
            String nickname,
            BigDecimal totalAsset,
            BigDecimal totalProfitRate,
            List<TopHolding> representativeStocks
    ) {
    }
}
