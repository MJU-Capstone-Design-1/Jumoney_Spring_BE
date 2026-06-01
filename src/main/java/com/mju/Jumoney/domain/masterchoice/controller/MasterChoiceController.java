package com.mju.Jumoney.domain.masterchoice.controller;

import com.mju.Jumoney.domain.master.dto.MasterResponse;
import com.mju.Jumoney.domain.master.service.MasterQueryService;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceBacktestResponse;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceRequest;
import com.mju.Jumoney.domain.masterchoice.dto.MasterChoiceResponse;
import com.mju.Jumoney.domain.masterchoice.service.MasterChoiceBacktestService;
import com.mju.Jumoney.domain.masterchoice.service.MasterChoiceService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "거장의 선택", description = "거장의 선택 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master-choice")
public class MasterChoiceController {

    private final MasterQueryService masterQueryService;
    private final MasterChoiceService masterChoiceService;
    private final MasterChoiceBacktestService masterChoiceBacktestService;

    @Operation(summary = "거장 정보 및 추천 조건 조회", description = "선택한 거장의 설명과 추천 조건 버튼 목록을 조회합니다.")
    @GetMapping("/masters/{masterId}")
    public ResponseEntity<ApiResponse<MasterResponse>> getMaster(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                masterQueryService.getMaster(masterId, getAuthenticatedUserId(userPrincipal))
        ));
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
    public ResponseEntity<ApiResponse<MasterChoiceResponse>> recommendMaster(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long masterId,
            @Valid @RequestBody MasterChoiceRequest request
    ) {
        getAuthenticatedUserId(userPrincipal);
        MasterChoiceResponse response = masterChoiceService.recommend(masterId, request);
        // TODO: 거장의 선택 결과 히스토리/최신 조회 기능이 필요해지면 saveMasterChoice 호출을 다시 연결한다.
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(
            summary = "거장의 선택 백테스팅 검증",
            description = "선택 종목의 직전 개장일까지 최근 1년 거래일에 현재 거장의 선택 추천 조건을 적용해 날짜별 조건 만족 여부를 조회합니다. "
                    + "일별 보조지표가 필요한 조건은 적재된 최신 거래일까지만 조회될 수 있습니다. "
                    + "프론트에서 차트를 함께 표시할 때는 모의투자 종목 차트 API를 period=ONE_YEAR, date=응답의 toDate로 호출하세요. "
                    + "정상 적재 상태에서는 응답의 toDate가 직전 개장일입니다."
    )
    @PostMapping("/masters/{masterId}/backtests/stocks/{stockCode}")
    public ResponseEntity<ApiResponse<MasterChoiceBacktestResponse>> backtestMaster(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long masterId,
            @PathVariable String stockCode,
            @Valid @RequestBody MasterChoiceRequest request
    ) {
        getAuthenticatedUserId(userPrincipal);
        MasterChoiceBacktestResponse response = masterChoiceBacktestService.backtest(masterId, stockCode, request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }
}
