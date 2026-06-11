package com.example.demo.security;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class SecurityStats {
    private final AtomicLong opaqueIntrospections = new AtomicLong();
    private final AtomicLong jwtDecodes = new AtomicLong();
    private final AtomicLong jwtSignatureFailures = new AtomicLong();
    private final AtomicLong totalOpaqueValidationNanos = new AtomicLong();
    private final AtomicLong totalJwtValidationNanos = new AtomicLong();

    public void onOpaqueIntrospection(Duration duration) {
        opaqueIntrospections.incrementAndGet();
        totalOpaqueValidationNanos.addAndGet(duration.toNanos());
    }

    public void onJwtDecode(Duration duration) {
        jwtDecodes.incrementAndGet();
        totalJwtValidationNanos.addAndGet(duration.toNanos());
    }

    public void onJwtSignatureFailure() {
        jwtSignatureFailures.incrementAndGet();
    }

    public Snapshot snapshot() {
        return new Snapshot(
                opaqueIntrospections.get(),
                jwtDecodes.get(),
                jwtSignatureFailures.get(),
                totalOpaqueValidationNanos.get(),
                totalJwtValidationNanos.get()
        );
    }

    public record Snapshot(
            long opaqueIntrospections,
            long jwtDecodes,
            long jwtSignatureFailures,
            long totalOpaqueValidationNanos,
            long totalJwtValidationNanos
    ) {
    }
}
