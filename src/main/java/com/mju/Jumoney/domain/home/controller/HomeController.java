package com.mju.Jumoney.domain.home.controller;

import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentChartResponse;
import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentRankingsResponse;
import com.mju.Jumoney.domain.home.dto.HomeMockInvestmentSummaryResponse;
import com.mju.Jumoney.domain.home.service.HomeMockInvestmentQueryService;
import com.mju.Jumoney.domain.home.service.HomeMockInvestmentRankingQueryService;
import com.mju.Jumoney.domain.stockterm.dto.TodayStockTermResponse;
import com.mju.Jumoney.domain.stockterm.service.StockTermQueryService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈", description = "홈 화면 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final StockTermQueryService stockTermQueryService;
    private final HomeMockInvestmentQueryService homeMockInvestmentQueryService;
    private final HomeMockInvestmentRankingQueryService homeMockInvestmentRankingQueryService;

    @Operation(summary = "오늘의 주식 용어 조회", description = "오늘 자정 기준으로 선정된 오늘의 주식 용어 1건을 조회합니다.")
    @GetMapping("/stock-term/today")
    public ResponseEntity<ApiResponse<TodayStockTermResponse>> getTodayStockTerm() {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, stockTermQueryService.getTodayStockTerm()));
    }

    @Operation(summary = "홈 모의투자 계좌 요약 조회", description = "총 매수금, 총 평가손익, 총 수익률과 대표 보유 종목 1건을 조회합니다. 계좌가 없으면 hasAccount=false를 반환합니다.")
    @GetMapping("/mock-investment-summary")
    public ResponseEntity<ApiResponse<HomeMockInvestmentSummaryResponse>> getMockInvestmentSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                homeMockInvestmentQueryService.getSummary(getAuthenticatedUserId(userPrincipal))
        ));
    }

    @Operation(summary = "홈 모의투자 대표 종목 차트 조회", description = "대표 보유 종목의 1일 차트를 조회합니다. 차트 조회 실패 시 빈 차트를 반환합니다.")
    @GetMapping("/mock-investment-summary/chart")
    public ResponseEntity<ApiResponse<HomeMockInvestmentChartResponse>> getMockInvestmentSummaryChart(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                homeMockInvestmentQueryService.getSummaryChart(getAuthenticatedUserId(userPrincipal))
        ));
    }

    @Operation(
            summary = "홈 모의투자 랭킹 조회",
            description = "전체 Top 5와 거장별 Top 5 랭킹을 함께 조회합니다. 랭킹 스냅샷은 1시간마다 갱신됩니다. 총 자산 내림차순으로 랭킹을 매기고 동률이면 총 수익률 순으로 정렬합니다. 대표 투자 기업 3개는 총 매수 금액 내림차순으로 선정합니다."
    )
    @GetMapping("/mock-investment-rankings")
    public ResponseEntity<ApiResponse<HomeMockInvestmentRankingsResponse>> getMockInvestmentRankings() {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                homeMockInvestmentRankingQueryService.getRankings()
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
