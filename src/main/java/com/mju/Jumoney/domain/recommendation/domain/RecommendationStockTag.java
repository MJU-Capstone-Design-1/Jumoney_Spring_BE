package com.mju.Jumoney.domain.recommendation.domain;

import com.mju.Jumoney.domain.recommendation.enums.RecommendationStockTagType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "recommendation_stock_tags",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_stock_tag",
                        columnNames = {"recommendation_stock_id", "tag_type", "tag_name"}
                )
        },
        indexes = {
                @Index(name = "idx_recommendation_stock_tag_stock", columnList = "recommendation_stock_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class RecommendationStockTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_stock_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_stock_id", nullable = false)
    private RecommendationStock recommendationStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag_type", nullable = false, length = 30)
    private RecommendationStockTagType tagType;

    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    public static RecommendationStockTag create(
            RecommendationStock recommendationStock,
            RecommendationStockTagType tagType,
            String tagName
    ) {
        return RecommendationStockTag.builder()
                .recommendationStock(recommendationStock)
                .tagType(tagType)
                .tagName(tagName)
                .build();
    }
}
