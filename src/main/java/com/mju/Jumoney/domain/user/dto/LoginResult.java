package com.mju.Jumoney.domain.user.dto;

// 컨트롤러가 Body에 담을 데이터(Access Token)와 쿠키에 구울 데이터(Refresh Token)를 서비스 계층으로부터 한 번에 전달받기 위한 내부용 DTO
public record LoginResult(
        AuthLoginResponse responseDto,
        String refreshToken
) {
}
