package com.mju.Jumoney.domain.mockinvestment.domain;

import com.mju.Jumoney.domain.mockinvestment.enums.OrderType;
import com.mju.Jumoney.domain.stock.domain.Stock;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity(name = "MockInvestmentOrder")
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_order_account_executed_at", columnList = "account_id,executed_at"),
                @Index(name = "idx_order_stock_id", columnList = "stock_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    @Column(name = "execution_price", nullable = false, precision = 20, scale = 4)
    private BigDecimal executionPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "total_execution_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalExecutionAmount;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    // ========== 정적 팩토리 메서드 ==========

    public static Order createDeposit(Account account, BigDecimal amount) {
        return Order.builder()
                .account(account)
                .stock(null)
                .orderType(OrderType.DEPOSIT)
                .executionPrice(amount)
                .quantity(1)
                .totalExecutionAmount(amount)
                .executedAt(LocalDateTime.now())
                .build();
    }

    public static Order createTrade(Account account, Stock stock, OrderType orderType, BigDecimal executionPrice, int quantity) {
        BigDecimal totalExecutionAmount = executionPrice.multiply(BigDecimal.valueOf(quantity));
        return Order.builder()
                .account(account)
                .stock(stock)
                .orderType(orderType)
                .executionPrice(executionPrice)
                .quantity(quantity)
                .totalExecutionAmount(totalExecutionAmount)
                .executedAt(LocalDateTime.now())
                .build();
    }
}
