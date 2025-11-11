package com.yerin.jobq.infra;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public final class Backoff {
    private Backoff() {}
    public static Duration expJitter(int retryCount, long baseMillis, long capMillis, double jitterRatio) {
        long exp = (long)(baseMillis * Math.pow(2, Math.max(0, retryCount)));
        long capped = Math.min(exp, capMillis);
        double jitter = 1.0 + (ThreadLocalRandom.current().nextDouble() * 2 - 1) * jitterRatio; // 1±r
        long withJitter = Math.max(0, (long)(capped * jitter));
        return Duration.ofMillis(withJitter);
    }
}
