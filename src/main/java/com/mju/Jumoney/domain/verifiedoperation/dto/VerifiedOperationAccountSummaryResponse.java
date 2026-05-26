package com.mju.Jumoney.domain.verifiedoperation.dto;

import java.util.List;

public record VerifiedOperationAccountSummaryResponse(
        String operationDescription,
        List<VerifiedOperationAccountResponse> accounts
) {
}
