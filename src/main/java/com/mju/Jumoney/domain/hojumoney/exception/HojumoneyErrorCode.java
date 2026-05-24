package com.mju.Jumoney.domain.hojumoney.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HojumoneyErrorCode implements BaseErrorCode {
    INVALID_SURVEY_SELECTION_COUNT(HttpStatus.BAD_REQUEST, "HOJUMONEY400_SURVEY_COUNT", "설문 선택지는 총 3개를 선택해야 합니다."),
    SURVEY_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "HOJUMONEY404_SURVEY_OPTION", "설문 선택지를 찾을 수 없습니다."),
    DUPLICATE_SURVEY_QUESTION_SELECTION(HttpStatus.BAD_REQUEST, "HOJUMONEY400_DUPLICATE_QUESTION", "각 설문 문항에서는 하나의 선택지만 선택할 수 있습니다."),
    MISSING_SURVEY_QUESTION_SELECTION(HttpStatus.BAD_REQUEST, "HOJUMONEY400_MISSING_QUESTION", "모든 설문 문항에 대해 선택지가 필요합니다."),
    RESTRICTED_SURVEY_OPTION_SELECTION(HttpStatus.BAD_REQUEST, "HOJUMONEY400_RESTRICTED_OPTION", "함께 선택할 수 없는 설문 선택지가 포함되어 있습니다."),
    INVALID_SURVEY_LOGIC_CODE(HttpStatus.BAD_REQUEST, "HOJUMONEY400_LOGIC_CODE", "설문 선택지의 로직 코드가 문항 타입과 일치하지 않습니다."),
    STOCK_INDICATOR_BASE_TIME_NOT_FOUND(HttpStatus.BAD_REQUEST, "HOJUMONEY400_STOCK_INDICATOR_BASE_TIME", "추천에 사용할 종목 지표 데이터가 없습니다."),
    STOCK_INDICATOR_REQUIRED_METRIC_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "HOJUMONEY500_STOCK_INDICATOR_REQUIRED_METRIC", "추천에 필요한 종목 지표 필수 값이 비어있습니다."),
    HTS_STOCK_BASE_DATE_NOT_FOUND(HttpStatus.BAD_REQUEST, "HOJUMONEY400_HTS_STOCK_BASE_DATE", "추천에 사용할 HTS 조건검색 데이터가 없습니다."),
    HOJUMONEY_PERSONA_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "HOJUMONEY500_PERSONA", "오늘의 호주머니 페르소나 데이터를 찾을 수 없습니다."),
    HOJUMONEY_RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "HOJUMONEY404_RESULT", "오늘의 호주머니 추천 결과를 찾을 수 없습니다."),
    INVALID_HOJUMONEY_LOGIC_CODE(HttpStatus.BAD_REQUEST, "HOJUMONEY400_INVALID_LOGIC_CODE", "오늘의 호주머니 추천 필터에 사용할 수 없는 설문 로직 코드입니다."),
    HOJUMONEY_AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "HOJUMONEY401_AUTHENTICATION_REQUIRED", "회원 로그인이 필요합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
