package com.mju.Jumoney.domain.mockinvestment.controller;

import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentAccountResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentDashboardResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderRequest;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentOrderResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentPortfolioListResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentSectorLeaderResponse;
import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentSectorStocksResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Operation(summary = "시장가 매수", description = "호출 시점의 현재가로 주식을 즉시 매수합니다. 장 중(9:00 ~ 15:30)에만 거래가 가능합니다.")
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

    @Operation(summary = "시장가 매도", description = "호출 시점의 현재가로 주식을 즉시 매도합니다. 장 중(9:00 ~ 15:30)에만 거래가 가능합니다.")
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
            @PathVariable Long sectorId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getSectorLeader(sectorId)
        ));
    }

    @Operation(summary = "내 보유 기업 리스트 조회", description = "현재 보유 중인 종목 목록을 최근 매매순으로 조회합니다.")
    @GetMapping("/portfolios")
    public ResponseEntity<ApiResponse<MockInvestmentPortfolioListResponse>> getPortfolios(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getPortfolios(getAuthenticatedUserId(userPrincipal))
        ));
    }

    @Operation(summary = "섹터별 기업 리스트 조회", description = "선택한 섹터에 속한 종목 목록을 시가총액 순으로 조회합니다.")
    @GetMapping("/sectors/{sectorId}/stocks")
    public ResponseEntity<ApiResponse<MockInvestmentSectorStocksResponse>> getSectorStocks(
            @PathVariable Long sectorId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getSectorStocks(sectorId)
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
