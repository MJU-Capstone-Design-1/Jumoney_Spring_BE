package com.mju.Jumoney.global.client.kis.dto;

// KIS REST API 응답이 공통으로 내려주는 성공 여부/메시지 필드입니다.
public interface KisApiResponse {

    String resultCode();

    String messageCode();

    String message();
}
