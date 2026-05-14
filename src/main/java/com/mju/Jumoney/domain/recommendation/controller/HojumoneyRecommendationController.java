package com.mju.Jumoney.domain.recommendation.controller;

import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationRequest;
import com.mju.Jumoney.domain.recommendation.dto.HojumoneyRecommendationResponse;
import com.mju.Jumoney.domain.recommendation.exception.RecommendationErrorCode;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recommendation", description = "추천 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class HojumoneyRecommendationController {

    private final HojumoneyRecommendationService hojumoneyRecommendationService;
    private final RecommendationSaveService recommendationSaveService;

    @Operation(summary = "오늘의 호주머니 추천", description = "설문 선택지 ID 3개를 기반으로 오늘의 호주머니 추천을 수행하여 추천 종목을 조회합니다.")
    @PostMapping("/hojumoney")
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
}
