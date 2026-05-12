package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;

import java.util.List;

public record SurveyOptionInitDto(
        String content,
        SurveyLogicCode logicCode,
        int displayOrder,
        List<SurveyLogicCode> restrictedLogicCodes,
        List<SurveyOptionIndicatorDescription> description
) {
}
