package com.mju.Jumoney.global.client.kis.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KisRateLimiter {

    private final long minIntervalMillis;
    private long lastRequestTimeMillis = 0L;

    public KisRateLimiter(@Value("${kis.rate-limit.min-interval-millis:250}") long minIntervalMillis) {
        this.minIntervalMillis = minIntervalMillis;
    }

    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTimeMillis;
        long waitMillis = minIntervalMillis - elapsed;

        if (waitMillis > 0) {
            try {
                Thread.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new KisApiException("[KIS] API 호출 제한 대기 중 인터럽트가 발생했습니다.", e);
            }
        }

        lastRequestTimeMillis = System.currentTimeMillis();
    }
}
