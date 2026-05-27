package com.mju.Jumoney.domain.user.controller;

import com.mju.Jumoney.domain.user.dto.UserInfoDTO;
import com.mju.Jumoney.domain.user.dto.UserUpdateDTO;
import com.mju.Jumoney.domain.user.service.UserService;
import com.mju.Jumoney.global.exception.CustomException;
import com.mju.Jumoney.global.jwt.JwtProperties;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.ErrorCode;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "서비스 닉네임 수정", description = "로그인 사용자가 앱 내에서 사용할 닉네임을 수정합니다.")
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UserUpdateDTO.Response>> updateNickname(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserUpdateDTO.Request request) {

        UserUpdateDTO.Response response = userService.updateServiceNickname(getAuthenticatedUserId(userPrincipal), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(summary = "내 정보 조회", description = "사용자 닉네임과 선택한 거장 ID를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoDTO.Response>> getUserInfo(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        UserInfoDTO.Response response = userService.getUserInfo(getAuthenticatedUserId(userPrincipal));
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @Operation(summary = "회원 탈퇴", description = "로그인 사용자를 탈퇴 처리하고 Refresh Token을 무효화합니다. 카카오 회원은 탈퇴 후 7일 내 재로그인하면 기존 계정이 복구되며, 7일이 지나면 관련 데이터와 함께 영구 삭제됩니다. 개발자용 임시 로그인 계정은 탈퇴 즉시 영구 삭제됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        userService.withdraw(getAuthenticatedUserId(userPrincipal));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createDeleteRefreshTokenCookie().toString())
                .body(ApiResponse.success(SuccessCode.OK));
    }

    // ========== 인증 메서드 ==========
    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }

    private ResponseCookie createDeleteRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path("/")
                .maxAge(0)
                .build();
    }
}
