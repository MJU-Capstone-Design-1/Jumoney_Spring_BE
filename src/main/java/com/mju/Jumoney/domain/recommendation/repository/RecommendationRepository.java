package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
}
