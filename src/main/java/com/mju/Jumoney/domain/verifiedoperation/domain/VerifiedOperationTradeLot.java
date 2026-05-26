package com.mju.Jumoney.domain.verifiedoperation.domain;

import com.mju.Jumoney.domain.mockinvestment.domain.Account;
import com.mju.Jumoney.domain.mockinvestment.domain.Order;
import com.mju.Jumoney.domain.stock.domain.Stock;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "verified_operation_trade_lots",
        indexes = {
                @Index(name = "idx_verified_operation_lot_account_code_due", columnList = "account_code,sell_due_at,remaining_quantity"),
                @Index(name = "idx_verified_operation_lot_account_id", columnList = "account_id"),
                @Index(name = "idx_verified_operation_lot_buy_order_id", columnList = "buy_order_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class VerifiedOperationTradeLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verified_operation_trade_lot_id")
    private Long id;

    @Column(name = "account_code", nullable = false, length = 100)
    private String accountCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_order_id", nullable = false)
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_order_id")
    private Order sellOrder;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "remaining_quantity", nullable = false)
    private int remainingQuantity;

    @Column(name = "bought_at", nullable = false)
    private LocalDateTime boughtAt;

    @Column(name = "sell_due_at")
    private LocalDateTime sellDueAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public static VerifiedOperationTradeLot create(
            String accountCode,
            Account account,
            Stock stock,
            Order buyOrder,
            int quantity,
            LocalDateTime boughtAt,
            LocalDateTime sellDueAt
    ) {
        return VerifiedOperationTradeLot.builder()
                .accountCode(accountCode)
                .account(account)
                .stock(stock)
                .buyOrder(buyOrder)
                .quantity(quantity)
                .remainingQuantity(quantity)
                .boughtAt(boughtAt)
                .sellDueAt(sellDueAt)
                .build();
    }

    public void close(Order sellOrder, LocalDateTime closedAt) {
        this.sellOrder = sellOrder;
        this.remainingQuantity = 0;
        this.closedAt = closedAt;
    }
}
