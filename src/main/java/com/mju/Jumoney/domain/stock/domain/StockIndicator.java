package com.mju.Jumoney.domain.stock.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "stock_indicators",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_indicator_stock_base_time",
                        columnNames = {"stock_id", "base_time"}
                )
        },
        indexes = {
                @Index(name = "idx_stock_indicator_stock_id", columnList = "stock_id"),
                @Index(name = "idx_stock_indicator_base_time", columnList = "base_time")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class StockIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "base_time", nullable = false, length = 6)
    private String baseTime;

    @Column(name = "market_cap", nullable = false)
    private Long marketCap;

    @Column(name = "debt_ratio", nullable = false, precision = 19, scale = 4)
    private BigDecimal debtRatio;

    @Column(name = "operating_profit", nullable = false)
    private Long operatingProfit;

    @Column(name = "operating_profit_growth_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal operatingProfitGrowthRate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal dps;

    @Column(name = "dividend_yield", nullable = false, precision = 19, scale = 4)
    private BigDecimal dividendYield;

    @Column(name = "payout_ratio", precision = 19, scale = 4)
    private BigDecimal payoutRatio;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal roe;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal per;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal pbr;

    @Column(name = "current_eps", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentEps;

    @Column(name = "last_year_eps", precision = 19, scale = 4)
    private BigDecimal lastYearEps;

    @Column(name = "current_sales", nullable = false)
    private Long currentSales;

    @Column(name = "last_year_sales")
    private Long lastYearSales;

    @Column(name = "margin_debt_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal marginDebtRate;

    @Column(name = "high52_week_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal high52WeekRate;

    @Column(name = "inst_net_buy20_days", nullable = false)
    private Long instNetBuy20Days;

    // ========== 정적 팩토리 메서드 ==========

    public static StockIndicator create(
            Stock stock,
            String baseTime,
            Long marketCap,
            BigDecimal debtRatio,
            Long operatingProfit,
            BigDecimal operatingProfitGrowthRate,
            BigDecimal dps,
            BigDecimal dividendYield,
            BigDecimal payoutRatio,
            BigDecimal roe,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal currentEps,
            BigDecimal lastYearEps,
            Long currentSales,
            Long lastYearSales,
            BigDecimal marginDebtRate,
            BigDecimal high52WeekRate,
            Long instNetBuy20Days
    ) {
        return StockIndicator.builder()
                .stock(stock)
                .baseTime(baseTime)
                .marketCap(marketCap)
                .debtRatio(debtRatio)
                .operatingProfit(operatingProfit)
                .operatingProfitGrowthRate(operatingProfitGrowthRate)
                .dps(dps)
                .dividendYield(dividendYield)
                .payoutRatio(payoutRatio)
                .roe(roe)
                .per(per)
                .pbr(pbr)
                .currentEps(currentEps)
                .lastYearEps(lastYearEps)
                .currentSales(currentSales)
                .lastYearSales(lastYearSales)
                .marginDebtRate(marginDebtRate)
                .high52WeekRate(high52WeekRate)
                .instNetBuy20Days(instNetBuy20Days)
                .build();
    }

    public void updateMetrics(
            Long marketCap,
            BigDecimal debtRatio,
            Long operatingProfit,
            BigDecimal operatingProfitGrowthRate,
            BigDecimal dps,
            BigDecimal dividendYield,
            BigDecimal payoutRatio,
            BigDecimal roe,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal currentEps,
            BigDecimal lastYearEps,
            Long currentSales,
            Long lastYearSales,
            BigDecimal marginDebtRate,
            BigDecimal high52WeekRate,
            Long instNetBuy20Days
    ) {
        this.marketCap = marketCap;
        this.debtRatio = debtRatio;
        this.operatingProfit = operatingProfit;
        this.operatingProfitGrowthRate = operatingProfitGrowthRate;
        this.dps = dps;
        this.dividendYield = dividendYield;
        this.payoutRatio = payoutRatio;
        this.roe = roe;
        this.per = per;
        this.pbr = pbr;
        this.currentEps = currentEps;
        this.lastYearEps = lastYearEps;
        this.currentSales = currentSales;
        this.lastYearSales = lastYearSales;
        this.marginDebtRate = marginDebtRate;
        this.high52WeekRate = high52WeekRate;
        this.instNetBuy20Days = instNetBuy20Days;
    }
}
