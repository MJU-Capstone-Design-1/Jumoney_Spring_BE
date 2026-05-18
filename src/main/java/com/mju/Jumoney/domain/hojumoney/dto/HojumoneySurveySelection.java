package com.mju.Jumoney.domain.hojumoney.dto;

import com.mju.Jumoney.domain.hojumoney.domain.SurveyOption;
import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;

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
