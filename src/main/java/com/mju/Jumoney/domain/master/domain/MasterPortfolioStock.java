package com.mju.Jumoney.domain.master.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "master_portfolio_stocks",
        indexes = {
                @Index(name = "idx_master_portfolio_stock_master_id", columnList = "master_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class MasterPortfolioStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "master_portfolio_stock_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_id", nullable = false)
    private Master master;

    @Column(name = "stock_name", nullable = false, length = 50)
    private String stockName;

    @Column(nullable = false, length = 50)
    private String sector;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal weight;

    public static MasterPortfolioStock create(
            Master master,
            String stockName,
            String sector,
            BigDecimal weight
    ) {
        return MasterPortfolioStock.builder()
                .master(master)
                .stockName(stockName)
                .sector(sector)
                .weight(weight)
                .build();
    }
}
