package com.mju.Jumoney.global.exception;

import com.mju.Jumoney.global.response.BaseErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException{

    private final BaseErrorCode errorCode;

    public CustomException(BaseErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

}
