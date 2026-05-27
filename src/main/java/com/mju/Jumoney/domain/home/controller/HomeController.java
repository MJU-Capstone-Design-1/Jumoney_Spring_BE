package com.mju.Jumoney.domain.home.controller;

import com.mju.Jumoney.domain.stockterm.dto.TodayStockTermResponse;
import com.mju.Jumoney.domain.stockterm.service.StockTermQueryService;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "홈", description = "홈 화면 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    private final StockTermQueryService stockTermQueryService;

    @Operation(summary = "오늘의 주식 용어 조회", description = "오늘 자정 기준으로 선정된 오늘의 주식 용어 1건을 조회합니다.")
    @GetMapping("/stock-term/today")
    public ResponseEntity<ApiResponse<TodayStockTermResponse>> getTodayStockTerm() {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, stockTermQueryService.getTodayStockTerm()));
    }
}
