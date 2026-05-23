package com.mju.Jumoney.domain.mockinvestment.controller;

import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentMinuteChartResponse;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentQueryService;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "모의투자 차트", description = "모의투자 종목 차트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-investments")
public class MockInvestmentChartController {

    private final MockInvestmentQueryService mockInvestmentQueryService;

    @Operation(summary = "모의투자 종목 분봉 차트 조회", description = "종목 코드 기준으로 DB 확정 1분봉을 조회하고, 조회 날짜가 오늘이면 Redis 미확정 분봉을 추가 병합합니다. 같은 시각 충돌 시 DB 확정 분봉을 우선합니다.")
    @GetMapping("/stocks/{stockCode}/charts/minute")
    public ResponseEntity<ApiResponse<MockInvestmentMinuteChartResponse>> getMinuteChart(
            @PathVariable String stockCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                mockInvestmentQueryService.getMinuteChart(stockCode, date)
        ));
    }
}
