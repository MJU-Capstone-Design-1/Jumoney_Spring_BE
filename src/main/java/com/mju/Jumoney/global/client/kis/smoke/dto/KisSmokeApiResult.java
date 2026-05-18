package com.mju.Jumoney.global.client.kis.smoke.dto;

public record KisSmokeApiResult(
        int step,
        String name,
        String trId,
        boolean success,
        int itemCount,
        Object sample,
        String errorMessage
) {
    public static KisSmokeApiResult success(int step, String name, String trId, int itemCount, Object sample) {
        return new KisSmokeApiResult(step, name, trId, true, itemCount, sample, null);
    }

    public static KisSmokeApiResult failure(int step, String name, String trId, String errorMessage) {
        return new KisSmokeApiResult(step, name, trId, false, 0, null, errorMessage);
    }
}
