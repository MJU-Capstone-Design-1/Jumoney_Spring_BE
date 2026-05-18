package com.mju.Jumoney.domain.hojumoney.dto;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;

import java.util.List;

public record SurveyOptionInitDto(
        String content,
        SurveyLogicCode logicCode,
        int displayOrder,
        List<SurveyLogicCode> restrictedLogicCodes,
        List<SurveyOptionIndicatorDescription> description
) {
}
