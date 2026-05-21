package com.mju.Jumoney.domain.master.controller;

import com.mju.Jumoney.domain.master.dto.*;
import com.mju.Jumoney.domain.master.service.MasterQueryService;
import com.mju.Jumoney.domain.master.service.MasterSelectionService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "거장 정보", description = "거장 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master")
public class MasterController {

    private final MasterQueryService masterQueryService;
    private final MasterSelectionService masterSelectionService;

    @Operation(summary = "거장 목록 조회", description = "거장 목록 화면에 필요한 거장 기본 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "거장 목록 조회 성공",
            content = @Content(examples = @ExampleObject(
                    name = "거장 목록 조회 성공",
                    value = """
                            {
                              "success": true,
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": [
                                {
                                  "masterId": 1,
                                  "masterCode": "WARREN_BUFFETT",
                                  "masterName": "워런 버핏",
                                  "tags": ["가치 투자", "경제적 해자"]
                                },
                                {
                                  "masterId": 2,
                                  "masterCode": "PETER_LYNCH",
                                  "masterName": "피터 린치",
                                  "tags": ["성장주", "생활 밀착형 투자"]
                                }
                              ]
                            }
                            """
            ))
    )
    @GetMapping("/masters")
    public ResponseEntity<ApiResponse<List<MasterListResponse>>> getMasters() {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, masterQueryService.getMasterList()));
    }

    @Operation(summary = "거장 상세정보 조회", description = "거장 상세정보(태그, 명언, 투자 철학, 투자 원칙)를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "거장 상세정보 조회 성공",
            content = @Content(examples = @ExampleObject(
                    name = "거장 상세정보 조회 성공",
                    value = """
                            {
                              "success": true,
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "masterId": 1,
                                "masterCode": "WARREN_BUFFETT",
                                "masterName": "워런 버핏",
                                "tags": ["가치 투자", "경제적 해자"],
                                "quote": "규칙 1: 절대 돈을 잃지 마라. 규칙 2: 규칙 1을 잊지 마라.",
                                "philosophy": {
                                  "title": "우량 기업 장기 보유",
                                  "description": "단순히 숫자만 보고 사고팔지 말고, 장기적으로 함께할 수 있는 훌륭한 기업에 투자해요."
                                },
                                "principles": [
                                  {
                                    "title": "경제적 해자",
                                    "description": "장기간 경쟁 우위를 유지할 수 있는 기업을 선호한다.",
                                    "details": []
                                  }
                                ]
                              }
                            }
                            """
            ))
    )
    @GetMapping("/masters/{masterId}/detail")
    public ResponseEntity<ApiResponse<MasterDetailResponse>> getMasterDetail(
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, masterQueryService.getMasterDetail(masterId)));
    }

    @Operation(summary = "거장 포트폴리오 차트 조회", description = "거장 포트폴리오의 분야별 차트와 투자 기업 비율을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "거장 포트폴리오 차트 조회 성공",
            content = @Content(examples = @ExampleObject(
                    name = "거장 포트폴리오 차트 조회 성공",
                    value = """
                            {
                              "success": true,
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "masterId": 1,
                                "masterCode": "WARREN_BUFFETT",
                                "masterName": "워런 버핏",
                                "basePeriod": "2025년 4분기 기준",
                                "sectorChart": [
                                  {
                                    "sector": "금융",
                                    "weight": 38.9
                                  },
                                  {
                                    "sector": "정보기술",
                                    "weight": 22.6
                                  }
                                ],
                                "companyRatioChart": [
                                  {
                                    "stockName": "애플",
                                    "weight": 22.6
                                  },
                                  {
                                    "stockName": "아메리칸 익스프레스",
                                    "weight": 20.4
                                  }
                                ]
                              }
                            }
                            """
            ))
    )
    @GetMapping("/masters/{masterId}/portfolio/chart")
    public ResponseEntity<ApiResponse<MasterPortfolioChartResponse>> getMasterPortfolioChart(
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, masterQueryService.getMasterPortfolioChart(masterId)));
    }

    @Operation(summary = "거장 포트폴리오 설명 조회", description = "거장 포트폴리오의 대표 투자 사례와 주식 리스트를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "거장 포트폴리오 설명 조회 성공",
            content = @Content(examples = @ExampleObject(
                    name = "거장 포트폴리오 설명 조회 성공",
                    value = """
                            {
                              "success": true,
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "masterId": 1,
                                "masterCode": "WARREN_BUFFETT",
                                "masterName": "워런 버핏",
                                "basePeriod": "2025년 4분기 기준",
                                "representativeCase": {
                                  "stockName": "코카콜라",
                                  "sector": "식음료 및 일상소비재",
                                  "investmentPeriod": "1988년",
                                  "investmentResult": "약 10년간 10배 이상 상승",
                                  "title": "가치 투자 및 장기 보유의 정석",
                                  "description": "1987년 블랙 먼데이 대폭락 이후 시장에 공포가 가득했던 시기였으나 버핏은 강력한 브랜드 파워와 변하지 않는 소비 패턴에 주목하여 과감하게 집중 투자를 단행했어요."
                                },
                                "stocks": [
                                  {
                                    "stockName": "애플",
                                    "sector": "정보기술",
                                    "weight": 22.6
                                  },
                                  {
                                    "stockName": "아메리칸 익스프레스",
                                    "sector": "금융",
                                    "weight": 20.4
                                  }
                                ]
                              }
                            }
                            """
            ))
    )
    @GetMapping("/masters/{masterId}/portfolio/description")
    public ResponseEntity<ApiResponse<MasterPortfolioDescriptionResponse>> getMasterPortfolioDescription(
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                masterQueryService.getMasterPortfolioDescription(masterId)
        ));
    }

    @Operation(summary = "거장 선택/변경", description = "로그인 사용자가 거장을 선택하거나 변경합니다. 변경 시 모의투자 계좌가 초기화됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "거장 선택/변경 성공",
            content = @Content(examples = @ExampleObject(
                    name = "거장 선택/변경 성공",
                    value = """
                            {
                              "success": true,
                              "code": "OK",
                              "message": "요청이 성공적으로 처리되었습니다.",
                              "data": {
                                "masterId": 1,
                                "masterCode": "WARREN_BUFFETT",
                                "masterName": "워런 버핏",
                                "selectionStatus": "INITIAL_SELECTION"
                              }
                            }
                            """
            ))
    )
    @PostMapping("/masters/{masterId}/selection")
    public ResponseEntity<ApiResponse<MasterSelectionResponse>> selectMaster(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long masterId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                masterSelectionService.selectMaster(getAuthenticatedUserId(userPrincipal), masterId)
        ));
    }

    // ========== 인증 메서드 ==========
    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }
}
