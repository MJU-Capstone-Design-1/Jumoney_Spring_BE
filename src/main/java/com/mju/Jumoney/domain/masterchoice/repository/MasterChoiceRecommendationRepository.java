package com.mju.Jumoney.domain.masterchoice.repository;

import com.mju.Jumoney.domain.masterchoice.domain.MasterChoiceRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MasterChoiceRecommendationRepository extends JpaRepository<MasterChoiceRecommendation, Long> {

    Optional<MasterChoiceRecommendation> findByRecommendationId(Long recommendationId);
}
