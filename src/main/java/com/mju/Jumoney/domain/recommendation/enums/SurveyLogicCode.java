package com.mju.Jumoney.domain.recommendation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SurveyLogicCode {
    CAPITAL_PROTECTION(SurveyQuestionType.INVESTMENT_PURPOSE),
    DIVIDEND_INCOME(SurveyQuestionType.INVESTMENT_PURPOSE),
    STEADY_GROWTH(SurveyQuestionType.INVESTMENT_PURPOSE),
    CAPITAL_GAIN(SurveyQuestionType.INVESTMENT_PURPOSE),

    STABILITY(SurveyQuestionType.RISK_PROFILE),
    SAFE_PURSUIT(SurveyQuestionType.RISK_PROFILE),
    PROFIT_PURSUIT(SurveyQuestionType.RISK_PROFILE),
    AGGRESSIVE(SurveyQuestionType.RISK_PROFILE),

    ULTRA_SHORT(SurveyQuestionType.INVESTMENT_HORIZON),
    SHORT(SurveyQuestionType.INVESTMENT_HORIZON),
    MID(SurveyQuestionType.INVESTMENT_HORIZON),
    LONG(SurveyQuestionType.INVESTMENT_HORIZON);

    private final SurveyQuestionType questionType;
}
