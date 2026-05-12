package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.SurveyOption;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyOptionRepository extends JpaRepository<SurveyOption, Long> {

    Optional<SurveyOption> findByLogicCode(SurveyLogicCode logicCode);
}
