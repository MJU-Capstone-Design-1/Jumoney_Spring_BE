package com.mju.Jumoney.domain.mockinvestment.controller;

import com.mju.Jumoney.domain.mockinvestment.dto.MockInvestmentAccountResponse;
import com.mju.Jumoney.domain.mockinvestment.service.MockInvestmentAccountService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "모의투자", description = "모의투자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-investments")
public class MockInvestmentController {

    private final MockInvestmentAccountService mockInvestmentAccountService;

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

    // ========== 인증 메서드 ==========
    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }
}
