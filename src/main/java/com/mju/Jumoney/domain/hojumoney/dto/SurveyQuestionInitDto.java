package com.mju.Jumoney.domain.hojumoney.dto;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyQuestionType;

import java.util.List;

public record SurveyQuestionInitDto(
        SurveyQuestionType questionType,
        String content,
        String description,
        int displayOrder,
        List<SurveyOptionInitDto> options
) {
}
