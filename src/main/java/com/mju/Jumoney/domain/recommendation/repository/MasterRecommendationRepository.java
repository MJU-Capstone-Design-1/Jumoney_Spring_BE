package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.MasterRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterRecommendationRepository extends JpaRepository<MasterRecommendation, Long> {

    Optional<MasterRecommendation> findByRecommendationId(Long recommendationId);
}
