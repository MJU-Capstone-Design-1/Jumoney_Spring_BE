package com.mju.Jumoney.domain.sector.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SectorErrorCode implements BaseErrorCode {
    SECTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "SECTOR404", "섹터를 찾을 수 없습니다."),
    INVALID_SECTOR(HttpStatus.BAD_REQUEST, "SECTOR400", "유효하지 않은 섹터입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
