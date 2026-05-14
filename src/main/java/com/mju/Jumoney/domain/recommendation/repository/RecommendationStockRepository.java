package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.RecommendationStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationStockRepository extends JpaRepository<RecommendationStock, Long> {
}
