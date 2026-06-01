package com.mju.Jumoney.domain.masterchoice.domain;

import com.mju.Jumoney.domain.stock.domain.Stock;
import com.mju.Jumoney.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "master_choice_backtest_daily_indicators",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_master_choice_backtest_daily_indicator_stock_trade_date",
                        columnNames = {"stock_id", "trade_date"}
                )
        },
        indexes = {
                @Index(name = "idx_master_choice_backtest_daily_indicator_stock_trade_date", columnList = "stock_id, trade_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterChoiceBacktestDailyIndicator extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_indicator_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "margin_debt_rate", precision = 19, scale = 4)
    private BigDecimal marginDebtRate;

    @Column(name = "institution_net_buy_quantity")
    private Long institutionNetBuyQuantity;

    public static MasterChoiceBacktestDailyIndicator create(Stock stock,
                                                            LocalDate tradeDate,
                                                            BigDecimal marginDebtRate,
                                                            Long institutionNetBuyQuantity) {
        return MasterChoiceBacktestDailyIndicator.builder()
                .stock(stock)
                .tradeDate(tradeDate)
                .marginDebtRate(marginDebtRate)
                .institutionNetBuyQuantity(institutionNetBuyQuantity)
                .build();
    }

    public void update(BigDecimal marginDebtRate, Long institutionNetBuyQuantity) {
        this.marginDebtRate = marginDebtRate;
        this.institutionNetBuyQuantity = institutionNetBuyQuantity;
    }
}
