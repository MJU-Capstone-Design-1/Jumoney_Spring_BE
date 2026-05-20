package com.mju.Jumoney.domain.mockinvestment.domain;

import com.mju.Jumoney.domain.stock.domain.Stock;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "portfolios",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_portfolio_account_stock", columnNames = {"account_id", "stock_id"})
        },
        indexes = {
                @Index(name = "idx_portfolio_account_id", columnList = "account_id"),
                @Index(name = "idx_portfolio_stock_id", columnList = "stock_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "average_purchase_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal averagePurchasePrice;

    @Column(name = "total_purchase_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalPurchaseAmount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ========== 정적 팩토리 메서드 ==========

    public static Portfolio create(Account account, Stock stock, int quantity, BigDecimal executionPrice) {
        BigDecimal totalPurchaseAmount = executionPrice.multiply(BigDecimal.valueOf(quantity));
        return Portfolio.builder()
                .account(account)
                .stock(stock)
                .quantity(quantity)
                .averagePurchasePrice(executionPrice)
                .totalPurchaseAmount(totalPurchaseAmount)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ========== 비즈니스 메서드 ==========

    public void buy(int buyQuantity, BigDecimal executionPrice) {
        BigDecimal buyAmount = executionPrice.multiply(BigDecimal.valueOf(buyQuantity));
        this.quantity += buyQuantity;
        this.totalPurchaseAmount = this.totalPurchaseAmount.add(buyAmount);
        this.averagePurchasePrice = this.totalPurchaseAmount.divide(
                BigDecimal.valueOf(this.quantity),
                4,
                RoundingMode.HALF_UP
        );
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal sell(int sellQuantity) {
        BigDecimal purchaseAmountToReduce = this.averagePurchasePrice.multiply(BigDecimal.valueOf(sellQuantity));
        this.quantity -= sellQuantity;
        this.totalPurchaseAmount = this.totalPurchaseAmount.subtract(purchaseAmountToReduce);
        this.updatedAt = LocalDateTime.now();
        return purchaseAmountToReduce;
    }
}
