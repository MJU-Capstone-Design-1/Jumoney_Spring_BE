package com.mju.Jumoney.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements BaseErrorCode {

    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "COMMON400", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 오류가 발생했습니다."),

    // 유효성 검증
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "COMMON400_VALIDATION", "입력값 검증에 실패했습니다."),
    INVALID_PARAMETER_TYPE(HttpStatus.BAD_REQUEST, "COMMON400_TYPE", "파라미터 타입이 올바르지 않습니다."),
    INVALID_REQUEST_FORMAT(HttpStatus.BAD_REQUEST, "COMMON400_FORMAT", "요청 형식이 올바르지 않습니다."),

    // JWT 인증
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH401_TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH401_INVALID_TOKEN", "유효하지 않은 토큰입니다.")
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;

}
