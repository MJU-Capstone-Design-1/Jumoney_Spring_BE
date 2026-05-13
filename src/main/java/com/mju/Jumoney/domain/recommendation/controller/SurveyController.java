package com.mju.Jumoney.domain.recommendation.controller;

import com.mju.Jumoney.domain.recommendation.dto.HojumoneySurveyResponse;
import com.mju.Jumoney.domain.recommendation.service.SurveyService;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Survey", description = "설문 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/surveys")
public class SurveyController {

    private final SurveyService surveyService;

    @Operation(summary = "오늘의 호주머니 설문 조회", description = "오늘의 호주머니 추천에 필요한 설문 문항과 선택지를 표시 순서대로 조회합니다.")
    @GetMapping("/hojumoney")
    public ResponseEntity<ApiResponse<HojumoneySurveyResponse>> getHojumoneySurvey() {
        HojumoneySurveyResponse response = surveyService.getHojumoneySurvey();
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
