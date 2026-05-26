package com.mju.Jumoney.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class UserUpdateDTO {

    @Builder
    @Schema(name = "UserNicknameUpdateRequestDTO", description = "닉네임 수정 요청")
    public record Request(
            @NotBlank(message = "서비스 닉네임은 필수입니다.")
            @Size(min = 2, max = 15, message = "닉네임은 2~15글자 사이여야 합니다.")
            String serviceNickname
    ) {
    }

    @Builder
    @Schema(name = "UserNicknameUpdateResponseDTO", description = "닉네임 수정 응답")
    public record Response(
            Long userId,
            String serviceNickname
    ) {
    }
}
