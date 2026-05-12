package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.enums.SurveyQuestionType;

import java.util.List;

public record SurveyQuestionInitDto(
        SurveyQuestionType questionType,
        String content,
        String description,
        int displayOrder,
        List<SurveyOptionInitDto> options
) {
}
