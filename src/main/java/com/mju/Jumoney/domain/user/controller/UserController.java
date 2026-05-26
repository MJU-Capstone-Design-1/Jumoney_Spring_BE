package com.mju.Jumoney.domain.user.controller;

import com.mju.Jumoney.domain.user.dto.UserInfoDTO;
import com.mju.Jumoney.domain.user.dto.UserUpdateDTO;
import com.mju.Jumoney.domain.user.service.UserService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

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

    // ========== 인증 메서드 ==========
    private Long getAuthenticatedUserId(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return userPrincipal.userId();
    }
}
