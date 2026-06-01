package com.mju.Jumoney.domain.masterchoice.domain;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "master_choice_backtest_financials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_master_choice_backtest_financial_stock_settlement",
                        columnNames = {"stock_id", "settlement_year_month"}
                )
        },
        indexes = {
                @Index(name = "idx_master_choice_backtest_financial_stock_available", columnList = "stock_id, available_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterChoiceBacktestFinancial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "backtest_financial_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "settlement_year_month", nullable = false, length = 6)
    private String settlementYearMonth;

    @Column(name = "available_date", nullable = false)
    private LocalDate availableDate;

    @Column(precision = 19, scale = 4)
    private BigDecimal roe;

    @Column(name = "current_eps", precision = 19, scale = 4)
    private BigDecimal currentEps;

    @Column(name = "last_year_eps", precision = 19, scale = 4)
    private BigDecimal lastYearEps;

    @Column(name = "debt_ratio", precision = 19, scale = 4)
    private BigDecimal debtRatio;

    @Column(name = "current_sales")
    private Long currentSales;

    @Column(name = "last_year_sales")
    private Long lastYearSales;

    @Column(name = "operating_profit")
    private Long operatingProfit;

    public static MasterChoiceBacktestFinancial create(BacktestFinancialMetrics metrics) {
        return MasterChoiceBacktestFinancial.builder()
                .stock(metrics.stock())
                .settlementYearMonth(metrics.settlementYearMonth())
                .availableDate(metrics.availableDate())
                .roe(metrics.roe())
                .currentEps(metrics.currentEps())
                .lastYearEps(metrics.lastYearEps())
                .debtRatio(metrics.debtRatio())
                .currentSales(metrics.currentSales())
                .lastYearSales(metrics.lastYearSales())
                .operatingProfit(metrics.operatingProfit())
                .build();
    }

    public void update(BacktestFinancialMetrics metrics) {
        this.availableDate = metrics.availableDate();
        this.roe = metrics.roe();
        this.currentEps = metrics.currentEps();
        this.lastYearEps = metrics.lastYearEps();
        this.debtRatio = metrics.debtRatio();
        this.currentSales = metrics.currentSales();
        this.lastYearSales = metrics.lastYearSales();
        this.operatingProfit = metrics.operatingProfit();
    }

    public record BacktestFinancialMetrics(
            Stock stock,
            String settlementYearMonth,
            LocalDate availableDate,
            BigDecimal roe,
            BigDecimal currentEps,
            BigDecimal lastYearEps,
            BigDecimal debtRatio,
            Long currentSales,
            Long lastYearSales,
            Long operatingProfit
    ) {
    }
}
