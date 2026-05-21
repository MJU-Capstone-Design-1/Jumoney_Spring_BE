package com.mju.Jumoney.domain.master.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MasterErrorCode implements BaseErrorCode {
    MASTER_NOT_FOUND(HttpStatus.NOT_FOUND, "MASTER404_NOT_FOUND", "거장을 찾을 수 없습니다."),
    MASTER_ALREADY_SELECTED(HttpStatus.CONFLICT, "MASTER409_ALREADY_SELECTED", "이미 선택한 거장입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
