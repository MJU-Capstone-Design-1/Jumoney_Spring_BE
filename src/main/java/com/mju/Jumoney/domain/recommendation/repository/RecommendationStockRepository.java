package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.RecommendationStock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationStockRepository extends JpaRepository<RecommendationStock, Long> {

    @EntityGraph(attributePaths = {"stock"})
    List<RecommendationStock> findByRecommendationIdOrderByRankAsc(Long recommendationId);
}
