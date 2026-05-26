package com.mju.Jumoney.domain.verifiedoperation.dto;

public record VerifiedOperationMasterAccountResponse(
        String operationDescription,
        VerifiedOperationAccountResponse account
) {
}
