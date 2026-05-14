package com.mju.Jumoney.domain.recommendation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SurveyLogicCode {
    CAPITAL_PROTECTION(SurveyQuestionType.INVESTMENT_PURPOSE, "안정적인 자산 보호"),
    DIVIDEND_INCOME(SurveyQuestionType.INVESTMENT_PURPOSE, "배당 수익"),
    STEADY_GROWTH(SurveyQuestionType.INVESTMENT_PURPOSE, "자산의 꾸준한 성장"),
    CAPITAL_GAIN(SurveyQuestionType.INVESTMENT_PURPOSE, "시세 차익"),

    STABILITY(SurveyQuestionType.RISK_PROFILE, "매우 낮음"),
    SAFE_PURSUIT(SurveyQuestionType.RISK_PROFILE, "낮음"),
    PROFIT_PURSUIT(SurveyQuestionType.RISK_PROFILE, "높음"),
    AGGRESSIVE(SurveyQuestionType.RISK_PROFILE, "매우 높음"),

    ULTRA_SHORT(SurveyQuestionType.INVESTMENT_HORIZON, "초단기(1일)"),
    SHORT(SurveyQuestionType.INVESTMENT_HORIZON, "단기(1주일)"),
    MID(SurveyQuestionType.INVESTMENT_HORIZON, "중기(3달)"),
    LONG(SurveyQuestionType.INVESTMENT_HORIZON, "장기(1년)");

    private final SurveyQuestionType questionType;
    private final String label;
}
