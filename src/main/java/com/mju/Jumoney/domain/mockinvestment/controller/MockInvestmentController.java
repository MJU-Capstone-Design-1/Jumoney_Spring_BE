package com.mju.Jumoney.domain.mockinvestment.controller;

import com.mju.Jumoney.domain.mockinvestment.dto.*;
import com.mju.Jumoney.domain.mockinvestment.enums.MockInvestmentStockSearchSortType;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentAccountService;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentCommandService;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentQueryService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "모의투자", description = "모의투자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-investments")
public class MockInvestmentController {

    private final MockInvestmentAccountService mockInvestmentAccountService;
    private final MockInvestmentCommandService mockInvestmentCommandService;
    private final MockInvestmentQueryService mockInvestmentQueryService;

    @Operation(summary = "모의투자 계좌 생성", description = "모의투자 탭 최초 진입 시, 로그인 사용자의 모의투자 계좌를 생성합니다. 계좌 생성 시 천만원이 지급되며, 이미 생성된 계좌가 있으면 기존 계좌 정보를 반환합니다.")
    @PostMapping("/accounts/init")
    public ResponseEntity<ApiResponse<MockInvestmentAccountResponse>> initializeAccount(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        MockInvestmentAccountResponse response = mockInvestmentAccountService.initializeAccount(getAuthenticatedUserId(userPrincipal));
        SuccessCode successCode = response.created() ? SuccessCode.CREATED : SuccessCode.OK;
        return ResponseEntity
                .status(successCode.getStatus())
                .body(ApiResponse.success(successCode, response));
    }

    @Operation(summary = "모의투자 메인 대시보드 조회", description = "예수금, 총 매입금, 총 평가금액, 총 자산, 총 손익, 총 수익률을 조회합니다.")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<MockInvestmentDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getDashboard(getAuthenticatedUserId(userPrincipal))
        ));
    }

    @Operation(summary = "시장가 매수", description = "호출 시점의 현재가로 주식을 즉시 매수합니다. 장 중(9:00 ~ 15:20)에만 거래가 가능합니다. 15:20~15:29는 한국 거래소의 장마감 동시호가 제도에 따라 매매가 불가하도록 처리했습니다.")
    @PostMapping("/orders/buy")
    public ResponseEntity<ApiResponse<MockInvestmentOrderResponse>> buy(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MockInvestmentOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentCommandService.buy(getAuthenticatedUserId(userPrincipal), request)
        ));
    }

    @Operation(summary = "시장가 매도", description = "호출 시점의 현재가로 주식을 즉시 매도합니다. 장 중(9:00 ~ 15:20)에만 거래가 가능합니다. 15:20~15:29는 한국 거래소의 장마감 동시호가 제도에 따라 매매가 불가하도록 처리했습니다. ")
    @PostMapping("/orders/sell")
    public ResponseEntity<ApiResponse<MockInvestmentOrderResponse>> sell(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MockInvestmentOrderRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentCommandService.sell(getAuthenticatedUserId(userPrincipal), request)
        ));
    }

    @Operation(summary = "관심 섹터 대장주 조회", description = "선택한 섹터의 대장주 1개를 조회합니다. (현재가, 전일 대비 등락률 포함)")
    @GetMapping("/sectors/{sectorId}/leader")
    public ResponseEntity<ApiResponse<MockInvestmentSectorLeaderResponse>> getSectorLeader(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sectorId
    ) {
        validateAccountExists(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getSectorLeader(sectorId)
        ));
    }

    @Operation(summary = "내 보유 종목 리스트 조회", description = "현재 보유 중인 종목 목록을 최근 매매순으로 조회합니다.")
    @GetMapping("/portfolios")
    public ResponseEntity<ApiResponse<MockInvestmentPortfolioListResponse>> getPortfolios(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getPortfolios(getAuthenticatedUserId(userPrincipal))
        ));
    }

    @Operation(summary = "모의투자 거래 이력 조회", description = "입금, 매수, 매도 이력을 최근 체결순으로 조회합니다.")
    @GetMapping("/orders/history")
    public ResponseEntity<ApiResponse<MockInvestmentOrderHistoryResponse>> getOrderHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getOrderHistory(getAuthenticatedUserId(userPrincipal))
        ));
    }

    @Operation(summary = "섹터별 종목 리스트 조회", description = "선택한 섹터에 속한 종목 목록을 시가총액 순으로 조회합니다.")
    @GetMapping("/sectors/{sectorId}/stocks")
    public ResponseEntity<ApiResponse<MockInvestmentSectorStocksResponse>> getSectorStocks(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long sectorId
    ) {
        validateAccountExists(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getSectorStocks(sectorId)
        ));
    }

    @Operation(summary = "모의투자 종목 상세 조회", description = "차트 제외, 종목 코드 기준으로 종목 기본 정보와 현재 시세, 최신 지표 중심의 상세 정보를 조회합니다.")
    @GetMapping("/stocks/{stockCode}")
    public ResponseEntity<ApiResponse<MockInvestmentStockDetailResponse>> getStockDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String stockCode
    ) {
        validateAccountExists(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getStockDetail(stockCode)
        ));
    }

    @Operation(summary = "종목 검색", description = "입력한 검색어가 종목명에 포함된 종목 목록을 조회합니다. 이름 순, 주가 높은 순, 주가 낮은 순, 시가총액 순, 거래대금 순으로 정렬할 수 있습니다.")
    @GetMapping("/stocks/search")
    public ResponseEntity<ApiResponse<MockInvestmentStockSearchResponse>> searchStocks(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "NAME_ASC") MockInvestmentStockSearchSortType sort
    ) {
        validateAccountExists(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.searchStocks(keyword, sort)
        ));
    }

    // ========== 인증 메서드 ==========
    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }

    private void validateAccountExists(UserPrincipal userPrincipal) {
        mockInvestmentAccountService.validateAccountExists(getAuthenticatedUserId(userPrincipal));
    }
}
