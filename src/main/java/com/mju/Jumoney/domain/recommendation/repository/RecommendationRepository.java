package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.Recommendation;
import com.mju.Jumoney.domain.recommendation.enums.RecommendationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findTopByUserIdAndRecommendationTypeOrderByCreatedAtDesc(Long userId, RecommendationType recommendationType);
}
