package com.mju.Jumoney.domain.stock.domain;

import com.mju.Jumoney.domain.stock.enums.HtsSearchType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "hts_stocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hts_stock_stock_search_type_base_date",
                        columnNames = {"stock_id", "search_type", "base_date"}
                )
        },
        indexes = {
                @Index(name = "idx_hts_stock_search_type_base_date", columnList = "search_type, base_date"),
                @Index(name = "idx_hts_stock_stock_id", columnList = "stock_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class HtsStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hts_stock_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 30)
    private HtsSearchType searchType;

    @Column(name = "base_date", nullable = false)
    private LocalDate baseDate;

    public static HtsStock create(Stock stock, HtsSearchType searchType, LocalDate baseDate) {
        return HtsStock.builder()
                .stock(stock)
                .searchType(searchType)
                .baseDate(baseDate)
                .build();
    }
}
