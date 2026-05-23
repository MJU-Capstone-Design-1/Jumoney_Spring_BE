package com.mju.Jumoney.domain.stock.domain;

import com.mju.Jumoney.domain.stock.enums.StockCandleIntervalType;
import com.mju.Jumoney.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_candles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_candle_stock_interval_time",
                        columnNames = {"stock_id", "interval_type", "candle_time"}
                )
        },
        indexes = {
                @Index(name = "idx_stock_candle_stock_interval_time", columnList = "stock_id, interval_type, candle_time"),
                @Index(name = "idx_stock_candle_code_interval_time", columnList = "stock_code, interval_type, candle_time")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class StockCandle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_candle_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "interval_type", nullable = false, length = 20)
    private StockCandleIntervalType intervalType;

    @Column(name = "candle_time", nullable = false)
    private LocalDateTime candleTime;

    @Column(name = "open_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal closePrice;

    @Column(nullable = false)
    private Long volume;

    @Column(name = "trade_amount")
    private Long tradeAmount;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    public static StockCandle createMinute(Stock stock,
                                           LocalDateTime candleTime,
                                           BigDecimal openPrice,
                                           BigDecimal highPrice,
                                           BigDecimal lowPrice,
                                           BigDecimal closePrice,
                                           Long volume,
                                           Long tradeAmount) {
        return createFinal(stock, StockCandleIntervalType.MINUTE, candleTime, openPrice, highPrice, lowPrice, closePrice, volume, tradeAmount);
    }

    public static StockCandle createFinal(Stock stock,
                                          StockCandleIntervalType intervalType,
                                          LocalDateTime candleTime,
                                          BigDecimal openPrice,
                                          BigDecimal highPrice,
                                          BigDecimal lowPrice,
                                          BigDecimal closePrice,
                                          Long volume,
                                          Long tradeAmount) {
        return StockCandle.builder()
                .stock(stock)
                .stockCode(stock.getStockCode())
                .intervalType(intervalType)
                .candleTime(candleTime)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .closePrice(closePrice)
                .volume(volume)
                .tradeAmount(tradeAmount)
                .isFinal(true)
                .build();
    }

    public void updateFinalCandle(BigDecimal openPrice,
                                  BigDecimal highPrice,
                                  BigDecimal lowPrice,
                                  BigDecimal closePrice,
                                  Long volume,
                                  Long tradeAmount) {
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.tradeAmount = tradeAmount;
        this.isFinal = true;
    }
}
