package com.mju.Jumoney.domain.stockterm.controller;

import com.mju.Jumoney.domain.stockterm.dto.*;
import com.mju.Jumoney.domain.stockterm.exception.StockTermErrorCode;
import com.mju.Jumoney.domain.stockterm.service.StockTermCommandService;
import com.mju.Jumoney.domain.stockterm.service.StockTermQueryService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "주식 용어", description = "주식 용어 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stock-terms")
public class StockTermController {

    private final StockTermQueryService stockTermQueryService;
    private final StockTermCommandService stockTermCommandService;

    @Operation(summary = "주식 용어 카테고리 목록 조회", description = "주식 용어 카테고리 ID와 이름 목록을 조회합니다.")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<StockTermCategoryResponse>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, stockTermQueryService.getCategories()));
    }

    @Operation(summary = "카테고리별 주식 용어 목록 조회", description = "카테고리별 용어명과 스크랩 여부를 조회합니다.")
    @GetMapping("/categories/{categoryId}/terms")
    public ResponseEntity<ApiResponse<StockTermCategoryTermsResponse>> getTermsByCategory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable int categoryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                stockTermQueryService.getTermsByCategory(getOptionalUserId(userPrincipal), categoryId)
        ));
    }

    @Operation(summary = "주식 용어 상세 조회", description = "용어 상세정보를 조회하고 학습 상태를 기록합니다.")
    @GetMapping("/terms/{termId}")
    public ResponseEntity<ApiResponse<StockTermDetailResponse>> getTermDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long termId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                stockTermQueryService.getTermDetail(getOptionalUserId(userPrincipal), termId)
        ));
    }

    @Operation(summary = "주식 용어 스크랩", description = "용어 스크랩 상태를 변경합니다. (토글 방식)")
    @PostMapping("/terms/{termId}/scrap")
    public ResponseEntity<ApiResponse<StockTermScrapToggleResponse>> toggleScrap(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long termId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                stockTermCommandService.toggleScrap(getAuthenticatedUserId(userPrincipal), termId)
        ));
    }

    @Operation(summary = "스크랩한 주식 용어 목록 조회", description = "로그인 사용자의 스크랩한 용어 목록을 최신순으로 조회합니다.")
    @GetMapping("/scraps")
    public ResponseEntity<ApiResponse<List<ScrappedStockTermResponse>>> getScraps(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                stockTermQueryService.getScrappedTerms(getAuthenticatedUserId(userPrincipal))
        ));
    }

    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(StockTermErrorCode.STOCK_TERM_AUTHENTICATION_REQUIRED);
        }
        return userPrincipal.userId();
    }

    private Long getOptionalUserId(UserPrincipal userPrincipal) {
        return userPrincipal == null ? null : userPrincipal.userId();
    }
}
