package com.mju.Jumoney.domain.recommendation.controller;

import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationRequest;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationResponse;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
import com.mju.Jumoney.domain.recommendation.service.HojumoneyRecommendationQueryService;
import com.mju.Jumoney.domain.recommendation.service.HojumoneyRecommendationService;
import com.mju.Jumoney.domain.recommendation.service.RecommendationSaveService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "오늘의 호주머니", description = "오늘의 호주머니 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hojumoney")
public class HojumoneyRecommendationController {

    private final HojumoneyRecommendationService hojumoneyRecommendationService;
    private final HojumoneyRecommendationQueryService hojumoneyRecommendationQueryService;
    private final RecommendationSaveService recommendationSaveService;

    @Operation(summary = "오늘의 호주머니 추천", description = "설문 선택지 ID 3개를 기반으로 오늘의 호주머니 추천을 수행하여 추천 종목을 조회합니다.")
    @PostMapping("/recommendations")
    public ResponseEntity<ApiResponse<HojumoneyRecommendationResponse>> recommend(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody HojumoneyRecommendationRequest request
    ) {
        if (userPrincipal == null) {
            throw new CustomException(RecommendationErrorCode.RECOMMENDATION_AUTHENTICATION_REQUIRED);
        }
        HojumoneyRecommendationResponse response = hojumoneyRecommendationService.recommend(request);
        Long recommendationId = recommendationSaveService.saveHojumoneyRecommendation(userPrincipal.userId(), request, response);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response.withRecommendationId(recommendationId)));
    }

    @Operation(summary = "오늘의 호주머니 추천 결과 조회", description = "로그인 사용자의 가장 최근 오늘의 호주머니 추천 결과를 조회합니다.")
    @GetMapping("/recommendations/latest")
    public ResponseEntity<ApiResponse<HojumoneyRecommendationResponse>> getLatest(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        if (userPrincipal == null) {
            throw new CustomException(RecommendationErrorCode.RECOMMENDATION_AUTHENTICATION_REQUIRED);
        }
        HojumoneyRecommendationResponse response = hojumoneyRecommendationQueryService.getLatestRecommendation(userPrincipal.userId());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
