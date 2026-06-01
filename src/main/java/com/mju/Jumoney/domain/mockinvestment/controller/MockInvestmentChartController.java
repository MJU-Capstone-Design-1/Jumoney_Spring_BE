package com.mju.Jumoney.domain.mockinvestment.controller;

import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentChartResponse;
import com.mju.Jumoney.domain.mockinvestment.enums.MockInvestmentChartPeriod;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentAccountService;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentQueryService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "모의투자 차트", description = "모의투자 종목 차트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-investments")
public class MockInvestmentChartController {

    private final MockInvestmentAccountService mockInvestmentAccountService;
    private final MockInvestmentQueryService mockInvestmentQueryService;

    @Operation(
            summary = "모의투자 종목 차트 조회",
            description = "period 기준 단일 차트 API입니다. ONE_DAY는 1분봉, ONE_WEEK는 30분봉, THREE_MONTHS/ONE_YEAR는 일봉, FIVE_YEARS는 주봉을 반환합니다. "
                    + "date 생략 시 직전 개장일 기준으로 보정합니다. 거장의 선택 백테스트 화면에서 최근 1년 차트를 함께 표시할 때는 period=ONE_YEAR, date=백테스트 응답의 toDate로 호출하세요. "
                    + "정상 적재 상태에서는 백테스트 응답의 toDate가 직전 개장일입니다."
    )
    @GetMapping("/stocks/{stockCode}/chart")
    public ResponseEntity<ApiResponse<MockInvestmentChartResponse>> getChart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String stockCode,
            @Parameter(description = "차트 기간", example = "ONE_DAY")
            @RequestParam MockInvestmentChartPeriod period,
            @Parameter(description = "차트 기준일. 생략 시 직전 개장일 기준으로 보정됩니다. 백테스트 화면의 ONE_YEAR 차트는 백테스트 응답의 toDate를 전달하세요. 정상 적재 상태에서는 이 값이 직전 개장일입니다.", example = "2026-05-29")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        mockInvestmentAccountService.validateAccountExists(getAuthenticatedUserId(userPrincipal));
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getChart(stockCode, period, date)
        ));
    }

    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }
}
