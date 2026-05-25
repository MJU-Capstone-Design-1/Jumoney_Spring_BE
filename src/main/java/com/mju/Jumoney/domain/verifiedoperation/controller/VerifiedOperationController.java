package com.mju.Jumoney.domain.verifiedoperation.controller;

import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountDetailResponse;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountSummaryResponse;
import com.mju.Jumoney.domain.verifiedoperation.service.VerifiedOperationQueryService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "검증용 모의 운용", description = "추천 검증용 모의 운용 계정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/verified-operations")
public class VerifiedOperationController {

    private final VerifiedOperationQueryService verifiedOperationQueryService;

    @Operation(summary = "모의 운용 계정 목록 조회", description = "추천 검증용 모의 운용 계정 8개의 성과 요약을 조회합니다.")
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<VerifiedOperationAccountSummaryResponse>> getAccounts(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                verifiedOperationQueryService.getAccounts()
        ));
    }

    @Operation(summary = "모의 운용 계정 상세 조회", description = "단일 모의 운용 계정의 보유 종목, 최근 주문 이력, 운용 조건을 조회합니다.")
    @GetMapping("/accounts/{accountCode}")
    public ResponseEntity<ApiResponse<VerifiedOperationAccountDetailResponse>> getAccount(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable String accountCode
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                verifiedOperationQueryService.getAccount(accountCode)
        ));
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
