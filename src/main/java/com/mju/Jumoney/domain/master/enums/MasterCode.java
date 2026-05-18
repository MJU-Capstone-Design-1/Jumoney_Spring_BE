package com.mju.Jumoney.domain.master.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum MasterCode {
    WARREN_BUFFETT("워런 버핏"),
    PETER_LYNCH("피터 린치"),
    RAY_DALIO("레이 달리오"),
    WILLIAM_ONEIL("윌리엄 오닐");

    private final String label;

    public static MasterCode fromLabel(String label) {
        return Arrays.stream(values())
                .filter(masterCode -> masterCode.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown master label: " + label));
    }
}
