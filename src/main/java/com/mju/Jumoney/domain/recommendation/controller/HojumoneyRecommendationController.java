package com.mju.Jumoney.domain.recommendation.controller;

import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationRequest;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationResponse;
import com.mju.Jumoney.domain.recommendation.service.HojumoneyRecommendationService;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recommendation", description = "추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class HojumoneyRecommendationController {

    private final HojumoneyRecommendationService hojumoneyRecommendationService;

    @Operation(summary = "오늘의 호주머니 추천 계산", description = "설문 선택지 ID 3개를 기반으로 오늘의 호주머니 추천 후보를 계산합니다.")
    @PostMapping("/hojumoney")
    public ResponseEntity<ApiResponse<HojumoneyRecommendationResponse>> recommend(
            @Valid @RequestBody HojumoneyRecommendationRequest request
    ) {
        HojumoneyRecommendationResponse response = hojumoneyRecommendationService.recommend(request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
