package com.mju.Jumoney.domain.mockinvestment.domain;

import com.mju.Jumoney.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_account_user", columnNames = {"user_id"})
        },
        indexes = {
                @Index(name = "idx_account_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "seed_money", nullable = false, precision = 20, scale = 4)
    private BigDecimal seedMoney;

    @Column(name = "cash_balance", nullable = false, precision = 20, scale = 4)
    private BigDecimal cashBalance;

    @Column(name = "total_purchase_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalPurchaseAmount;

    @Column(name = "total_asset", nullable = false, precision = 20, scale = 4)
    private BigDecimal totalAsset;

    @Column(name = "total_profit_rate", precision = 10, scale = 4)
    private BigDecimal totalProfitRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ========== 정적 팩토리 메서드 ==========

    public static Account create(User user, BigDecimal seedMoney) {
        return Account.builder()
                .user(user)
                .seedMoney(seedMoney)
                .cashBalance(seedMoney)
                .totalPurchaseAmount(BigDecimal.ZERO)
                .totalAsset(seedMoney)
                .totalProfitRate(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ========== 비즈니스 메서드 ==========

    public void increaseCashBalance(BigDecimal amount) {
        this.cashBalance = this.cashBalance.add(amount);
    }

    public void decreaseCashBalance(BigDecimal amount) {
        this.cashBalance = this.cashBalance.subtract(amount);
    }

    public void increaseTotalPurchaseAmount(BigDecimal amount) {
        this.totalPurchaseAmount = this.totalPurchaseAmount.add(amount);
    }

    public void decreaseTotalPurchaseAmount(BigDecimal amount) {
        this.totalPurchaseAmount = this.totalPurchaseAmount.subtract(amount);
    }

    public void updateTotalAsset(BigDecimal totalAsset, BigDecimal totalProfitRate) {
        this.totalAsset = totalAsset;
        this.totalProfitRate = totalProfitRate;
    }
}
