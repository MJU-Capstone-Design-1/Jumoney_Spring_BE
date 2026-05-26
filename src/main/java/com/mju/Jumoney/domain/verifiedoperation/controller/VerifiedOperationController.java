package com.mju.Jumoney.domain.verifiedoperation.controller;

import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationAccountSummaryResponse;
import com.mju.Jumoney.domain.verifiedoperation.dto.VerifiedOperationMasterAccountResponse;
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

    @Operation(summary = "오늘의 호주머니 모의 운용 계정 목록 조회", description = "오늘의 호주머니 추천 검증용 모의 운용 계정 4개를 조회합니다.")
    @GetMapping("/hojumoney/accounts")
    public ResponseEntity<ApiResponse<VerifiedOperationAccountSummaryResponse>> getHojumoneyAccounts(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                verifiedOperationQueryService.getHojumoneyAccounts()
        ));
    }

    @Operation(summary = "거장의 선택 모의 운용 계정 조회", description = "거장의 선택 추천 검증용 모의 운용 계정을 조회합니다.")
    @GetMapping("/master-choice/masters/{masterCode}/account")
    public ResponseEntity<ApiResponse<VerifiedOperationMasterAccountResponse>> getMasterChoiceAccount(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable MasterCode masterCode
    ) {
        validateAuthenticated(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success(
                SuccessCode.OK,
                verifiedOperationQueryService.getMasterChoiceAccount(masterCode)
        ));
    }

    private void validateAuthenticated(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}
