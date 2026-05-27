package com.mju.Jumoney.domain.stockterm.domain;

import com.mju.Jumoney.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "today_stock_terms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_today_stock_terms_target_date", columnNames = "target_date")
        },
        indexes = {
                @Index(name = "idx_today_stock_terms_stock_term_id", columnList = "stock_term_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class TodayStockTerm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "today_stock_term_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_term_id", nullable = false)
    private StockTerm stockTerm;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    public static TodayStockTerm create(StockTerm stockTerm, LocalDate targetDate) {
        return TodayStockTerm.builder()
                .stockTerm(stockTerm)
                .targetDate(targetDate)
                .build();
    }
}
