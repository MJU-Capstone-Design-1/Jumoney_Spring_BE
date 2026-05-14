package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;

public record HojumoneyPersonaInitDto(
        SurveyLogicCode investmentPurpose,
        SurveyLogicCode riskProfile,
        SurveyLogicCode investmentHorizon,
        String personaName,
        String personaDescription
) {
}
