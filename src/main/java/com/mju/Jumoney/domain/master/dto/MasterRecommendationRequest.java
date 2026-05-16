package com.mju.Jumoney.domain.master.dto;

import com.mju.Jumoney.domain.sector.enums.SectorType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MasterRecommendationRequest(
        @Schema(description = "거장 추천 조건 ID 목록. GET /api/master-choice/masters/{masterId}의 options[].optionId만 전달합니다. 비우면 해당 거장의 모든 조건을 적용합니다.")
        List<Long> selectedOptionIds,

        @Schema(description = "섹터 선택이 필요한 조건(LYNCH_SECTOR, DALIO_ALL_WEATHER)을 선택한 경우에만 전달합니다.")
        List<SectorType> sectorTypes
) {
}
