package com.mju.Jumoney.domain.sector.enums;

import com.mju.Jumoney.domain.sector.exception.SectorErrorCode;
import com.mju.Jumoney.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum SectorType {
    IT_SEMICONDUCTOR("IT/반도체"),
    AUTOMOBILE_TRANSPORT("자동차/운송"),
    ENERGY_CHEMISTRY("에너지/화학"),
    BIO_HEALTHCARE("바이오/헬스케어"),
    SHIPBUILDING_MACHINERY("조선/기계"),
    FINANCE("금융"),
    COMMUNICATION("커뮤니케이션"),
    STEEL_MATERIALS("철강/소재"),
    CONSTRUCTION_UTILITY("건설/유틸리티"),
    ESSENTIAL_CONSUMER("필수소비재");

    private final String description;

    public static SectorType fromDescription(String description) {
        return Arrays.stream(values())
                .filter(s -> s.description.equals(description.trim()))
                .findFirst()
                .orElseThrow(() -> new CustomException(SectorErrorCode.INVALID_SECTOR));
    }
}
