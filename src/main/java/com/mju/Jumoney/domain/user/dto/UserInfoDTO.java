package com.mju.Jumoney.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

public class UserInfoDTO {

    @Builder
    @Schema(name = "UserInfoResponseDTO", description = "사용자 정보 조회 응답")
    public record Response(
            String nickname,
            Long selectedMasterId
    ) {
    }
}
