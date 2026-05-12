package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.SurveyOptionRestriction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyOptionRestrictionRepository extends JpaRepository<SurveyOptionRestriction, Long> {

    boolean existsBySourceOptionIdAndRestrictedOptionId(Long sourceOptionId, Long restrictedOptionId);

    List<SurveyOptionRestriction> findBySourceOptionId(Long sourceOptionId);
}
