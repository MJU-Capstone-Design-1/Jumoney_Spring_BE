package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.domain.SurveyOption;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;

import java.util.List;

public record HojumoneySurveySelection(
        SurveyOption investmentPurposeOption,
        SurveyLogicCode investmentPurpose,
        SurveyOption riskProfileOption,
        SurveyLogicCode riskProfile,
        SurveyOption investmentHorizonOption,
        SurveyLogicCode investmentHorizon,
        List<SurveyOption> selectedOptions
) {
}
