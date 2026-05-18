package com.mju.Jumoney.domain.hojumoney.controller;

import com.mju.Jumoney.domain.hojumoney.dto.HojumoneySurveyResponse;
import com.mju.Jumoney.domain.hojumoney.service.HojumoneySurveyService;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "오늘의 호주머니", description = "오늘의 호주머니 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hojumoney")
public class HojumoneySurveyController {

    private final HojumoneySurveyService hojumoneySurveyService;

    @Operation(summary = "오늘의 호주머니 설문 조회", description = "오늘의 호주머니 추천에 필요한 설문 문항과 선택지를 표시 순서대로 조회합니다.")
    @GetMapping("/survey")
    public ResponseEntity<ApiResponse<HojumoneySurveyResponse>> getHojumoneySurvey() {
        HojumoneySurveyResponse response = hojumoneySurveyService.getHojumoneySurvey();
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
