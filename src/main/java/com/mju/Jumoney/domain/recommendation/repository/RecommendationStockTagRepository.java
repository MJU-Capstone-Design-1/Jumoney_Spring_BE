package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.RecommendationStockTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RecommendationStockTagRepository extends JpaRepository<RecommendationStockTag, Long> {

    List<RecommendationStockTag> findByRecommendationStockIdIn(Collection<Long> recommendationStockIds);
}
