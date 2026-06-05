package com.mju.Jumoney.global.jwt;

public record UserPrincipal(Long userId, String role, String nickname) {
}
