package com.mju.Jumoney.domain.recommendation.dto;

import com.mju.Jumoney.domain.recommendation.enums.SurveyQuestionType;
import com.mju.Jumoney.domain.recommendation.enums.SurveyLogicCode;

import java.util.List;

public record HojumoneySurveyResponse(
        List<SurveyQuestionResponse> questions
) {

    public record SurveyQuestionResponse(
            Long questionId,
            SurveyQuestionType questionType,
            String content,
            String description,
            int displayOrder,
            List<SurveyOptionResponse> options
    ) {
    }

    public record SurveyOptionResponse(
            Long optionId,
            SurveyLogicCode logicCode,
            String content,
            int displayOrder,
            List<Long> restrictedOptionIds,
            List<SurveyOptionIndicatorDescription> description
    ) {
    }
}
