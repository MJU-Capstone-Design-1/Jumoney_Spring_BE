package com.mju.Jumoney.domain.recommendation.repository;

import com.mju.Jumoney.domain.recommendation.domain.HojumoneyPersona;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HojumoneyPersonaRepository extends JpaRepository<HojumoneyPersona, Long> {

    Optional<HojumoneyPersona> findByInvestmentPurposeAndRiskProfileAndInvestmentHorizon(
            SurveyLogicCode investmentPurpose,
            SurveyLogicCode riskProfile,
            SurveyLogicCode investmentHorizon
    );
}
