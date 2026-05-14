package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.HojumoneyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HojumoneyRecommendationRepository extends JpaRepository<HojumoneyRecommendation, Long> {

    Optional<HojumoneyRecommendation> findByRecommendationId(Long recommendationId);
}
