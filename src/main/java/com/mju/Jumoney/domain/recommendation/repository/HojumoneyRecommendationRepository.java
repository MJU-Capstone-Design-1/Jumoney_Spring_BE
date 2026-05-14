package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.HojumoneyRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HojumoneyRecommendationRepository extends JpaRepository<HojumoneyRecommendation, Long> {
}
