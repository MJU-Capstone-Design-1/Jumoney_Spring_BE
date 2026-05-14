package com.mju.Jumoney.domain.recommendation.domain;

import com.mju.Jumoney.domain.stock.domain.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "recommendation_stocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_stock_recommendation_rank",
                        columnNames = {"recommendation_id", "recommendation_rank"}
                )
        },
        indexes = {
                @Index(name = "idx_recommendation_stock_recommendation", columnList = "recommendation_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class RecommendationStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_stock_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "recommendation_rank", nullable = false)
    private int rank;

    @Column(name = "matched_condition_count", nullable = false)
    private int matchedConditionCount;

    @Column(name = "sort_metric_key", nullable = false, length = 50)
    private String sortMetricKey;

    @Column(name = "sort_metric_value", precision = 20, scale = 4)
    private BigDecimal sortMetricValue;

    @Column(name = "current_price", precision = 20, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "change_rate", precision = 10, scale = 4)
    private BigDecimal changeRate;

    public static RecommendationStock create(
            Recommendation recommendation,
            Stock stock,
            int rank,
            int matchedConditionCount,
            String sortMetricKey,
            BigDecimal sortMetricValue,
            BigDecimal currentPrice,
            BigDecimal changeRate
    ) {
        return RecommendationStock.builder()
                .recommendation(recommendation)
                .stock(stock)
                .rank(rank)
                .matchedConditionCount(matchedConditionCount)
                .sortMetricKey(sortMetricKey)
                .sortMetricValue(sortMetricValue)
                .currentPrice(currentPrice)
                .changeRate(changeRate)
                .build();
    }
}
