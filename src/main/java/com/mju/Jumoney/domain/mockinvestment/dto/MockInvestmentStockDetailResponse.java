package com.mju.Jumoney.domain.mockinvestment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record MockInvestmentStockDetailResponse(
        @Schema(description = "종목 ID", example = "1")
        Long stockId,
        @Schema(description = "종목 코드", example = "005930")
        String stockCode,
        @Schema(description = "종목명", example = "삼성전자")
        String stockName,
        @Schema(description = "섹터명", example = "IT_SEMICONDUCTOR")
        String sector,
        @Schema(description = "대장주 여부", example = "true")
        boolean isMarketLeader,
        @Schema(description = "종목 태그 (섹터 태그, 대장주 태그)")
        List<String> tags,
        @Schema(description = "시세 지표")
        PriceInfo price,
        @Schema(description = "투자 지표")
        InvestmentMetrics investmentMetrics,
        @Schema(description = "재무 지표")
        FinancialMetrics financialMetrics,
        @Schema(description = "종목 설명")
        List<String> description
) {
    public record PriceInfo(
            @Schema(description = "현재가", example = "73500")
            BigDecimal currentPrice,
            @Schema(description = "전일 대비 등락률", example = "1.66")
            BigDecimal changeRate,
            @Schema(description = "시가총액", example = "438000000000000")
            Long marketCap,
            @Schema(description = "누적 거래대금", example = "845000000000")
            Long accumulatedTradeAmount
    ) {
    }

    public record InvestmentMetrics(
            @Schema(description = "주가순자산비율(PBR)", example = "1.45")
            BigDecimal pbr,
            @Schema(description = "주가수익비율(PER)", example = "18.2")
            BigDecimal per,
            @Schema(description = "자기자본이익률(ROE)", example = "12.8")
            BigDecimal roe,
            @Schema(description = "배당수익률", example = "2.15")
            BigDecimal dividendYield,
            @Schema(description = "배당성향", example = "35.9")
            BigDecimal payoutRatio,
            @Schema(description = "체결강도", example = "121.4")
            BigDecimal executionStrength,
            @Schema(description = "최근 20거래일 기관 순매수 수량", example = "1523000")
            Long instNetBuy20Days
    ) {
    }

    public record FinancialMetrics(
            @Schema(description = "매출액", example = "279600000000000")
            Long sales,
            @Schema(description = "영업이익", example = "6540000000000")
            Long operatingProfit,
            @Schema(description = "부채비율", example = "24.1")
            BigDecimal debtRatio
    ) {
    }
}
