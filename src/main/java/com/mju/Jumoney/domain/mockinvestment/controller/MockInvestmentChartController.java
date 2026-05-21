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

    @Operation(summary = "모의투자 종목 분봉 차트 조회", description = "종목 코드 기준으로 DB에 저장된 확정 1분봉 차트를 조회합니다. 현재 구현은 Redis 미확정 분봉을 포함하지 않으며 includesRealtime=false로 반환합니다.")
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
