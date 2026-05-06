package com.mju.Jumoney.global.client.kis.core;

// KIS API 실패 예외
public class KisApiException extends RuntimeException {

    public KisApiException(String message) {
        super(message);
    }

    public KisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
