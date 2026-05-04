package com.mju.Jumoney.domain.stock.domain;

import com.mju.Jumoney.domain.sector.domain.Sector;
import com.mju.Jumoney.domain.stock.enums.MarketType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(
    name = "stocks",
    indexes = { @Index(name = "idx_stock_code", columnList = "stock_code", unique = true) }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(name = "stock_code", nullable = false, length = 10)
    private String stockCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MarketType marketType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> description;

    @Column(name = "is_market_leader", nullable = false)
    private boolean isMarketLeader;

    // ========== 정적 팩토리 메서드 ==========

    public static Stock create(Sector sector, String stockCode, String name,
                               MarketType marketType, List<String> description, boolean isMarketLeader) {
        return Stock.builder()
                .sector(sector)
                .stockCode(stockCode)
                .name(name)
                .marketType(marketType)
                .description(description)
                .isMarketLeader(isMarketLeader)
                .build();
    }
}
