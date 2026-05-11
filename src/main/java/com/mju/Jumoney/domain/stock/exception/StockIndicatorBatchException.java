package com.mju.Jumoney.domain.stock.exception;

import com.mju.Jumoney.global.exception.CustomException;

public class StockIndicatorBatchException extends CustomException {

    public StockIndicatorBatchException(StockErrorCode errorCode) {
        super(errorCode);
    }

    public StockIndicatorBatchException(StockErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
