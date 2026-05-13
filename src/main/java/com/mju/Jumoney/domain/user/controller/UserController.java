package com.mju.Jumoney.domain.user.controller;

import com.mju.Jumoney.domain.user.dto.UserUpdateDTO;
import com.mju.Jumoney.domain.user.service.UserService;
import com.mju.Jumoney.global.jwt.UserPrincipal;
import com.mju.Jumoney.global.response.ApiResponse;
import com.mju.Jumoney.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "서비스 닉네임 설정", description = "최초 로그인 후 앱 내에서 사용할 닉네임을 설정합니다.")
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<UserUpdateDTO.Response>> updateNickname(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UserUpdateDTO.Request request) {

        UserUpdateDTO.Response response = userService.updateServiceNickname(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
