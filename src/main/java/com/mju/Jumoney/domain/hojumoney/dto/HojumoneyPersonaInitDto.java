package com.mju.Jumoney.domain.hojumoney.dto;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;

public record HojumoneyPersonaInitDto(
        SurveyLogicCode investmentPurpose,
        SurveyLogicCode riskProfile,
        SurveyLogicCode investmentHorizon,
        String personaName,
        String personaDescription
) {
}
