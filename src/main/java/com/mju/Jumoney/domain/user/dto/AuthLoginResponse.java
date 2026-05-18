package com.mju.Jumoney.domain.user.dto;

// 클라이언트에게 전달할 로그인 응답용 DTO
public record AuthLoginResponse(
        String accessToken,
        Long userId,
        String nickname,
        boolean isNewMember
) {
}
