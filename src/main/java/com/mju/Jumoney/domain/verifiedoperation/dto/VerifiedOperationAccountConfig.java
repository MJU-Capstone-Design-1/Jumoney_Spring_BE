package com.mju.Jumoney.domain.verifiedoperation.dto;

import com.mju.Jumoney.domain.hojumoney.enums.SurveyLogicCode;
import com.mju.Jumoney.domain.master.enums.MasterCode;
import com.mju.Jumoney.domain.master.enums.MasterOptionLogicCode;
import com.mju.Jumoney.domain.verifiedoperation.enums.VerifiedOperationAccountType;

import java.util.List;

public record VerifiedOperationAccountConfig(
        String accountCode,
        String accountName,
        VerifiedOperationAccountType type,
        List<SurveyLogicCode> hojumoneyConditions,
        MasterCode masterCode,
        List<MasterOptionLogicCode> masterConditions
) {
    public String providerId() {
        return "verified-operation:" + accountCode;
    }
}
