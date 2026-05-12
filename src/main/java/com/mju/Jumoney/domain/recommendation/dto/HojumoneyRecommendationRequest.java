package com.mju.Jumoney.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record HojumoneyRecommendationRequest(
        @Schema(description = "오늘의 호주머니 설문 선택지 ID 목록. 각 질문에서 1개씩 총 3개를 전달합니다.", example = "[1,5,9]")
        @NotEmpty(message = "선택지 ID 목록은 비어 있을 수 없습니다.")
        List<Long> selectedOptionIds
) {
}
