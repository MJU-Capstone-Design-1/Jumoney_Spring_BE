package com.mju.Jumoney.domain.master.controller;

import com.mju.Jumoney.domain.master.dto.MasterRecommendationRequest;
import com.mju.Jumoney.domain.master.dto.MasterRecommendationResponse;
import com.mju.Jumoney.domain.master.dto.MasterResponse;
import com.mju.Jumoney.domain.master.service.MasterQueryService;
import com.mju.Jumoney.domain.master.service.MasterRecommendationService;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "거장의 선택", description = "거장의 선택 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master-choice")
public class MasterController {

    private final MasterQueryService masterQueryService;
    private final MasterRecommendationService masterRecommendationService;

    @Operation(summary = "거장 정보 및 추천 조건 조회", description = "선택한 거장의 설명과 추천 조건 버튼 목록을 조회합니다.")
    @GetMapping("/masters/{masterId}")
    public ResponseEntity<ApiResponse<MasterResponse>> getMaster(
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, masterQueryService.getMaster(masterId)));
    }

    @Operation(
            summary = "거장의 선택 추천",
            description = "선택한 조건 ID 목록과 섹터 목록을 기반으로 거장의 선택 추천을 수행하여 추천 종목을 조회합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "워런 버핏",
                                    value = """
                                            {
                                              "selectedOptionIds": [1, 2, 3]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "피터 린치 - 섹터 선택 포함",
                                    value = """
                                            {
                                              "selectedOptionIds": [6, 7, 10],
                                              "sectorTypes": ["IT_SEMICONDUCTOR"]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "레이 달리오 - 올웨더 섹터 선택 포함",
                                    value = """
                                            {
                                              "selectedOptionIds": [11, 12, 13],
                                              "sectorTypes": ["IT_SEMICONDUCTOR", "FINANCE"]
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "윌리엄 오닐",
                                    value = """
                                            {
                                              "selectedOptionIds": [16, 17, 18]
                                            }
                                            """
                            )
                    })
            )
    )
    @PostMapping("/masters/{masterId}/recommendations")
    public ResponseEntity<ApiResponse<MasterRecommendationResponse>> recommendMaster(
            @PathVariable Long masterId,
            @Valid @RequestBody MasterRecommendationRequest request
    ) {
        MasterRecommendationResponse response = masterRecommendationService.recommend(masterId, request);
        // TODO: 거장의 선택 결과 히스토리/최신 조회 기능이 필요해지면 saveMasterRecommendation 호출을 다시 연결한다.
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
