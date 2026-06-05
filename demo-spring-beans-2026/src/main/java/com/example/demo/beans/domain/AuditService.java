package com.example.demo.beans.domain;

import org.springframework.stereotype.Service;

@Service
public final class AuditService {
    private final ClockProvider clockProvider;

    public AuditService(ClockProvider clockProvider) {
        this.clockProvider = clockProvider;
    }

    public String stamp(String input) {
        return clockProvider.nowEpochMillis() + ":" + input;
    }
}
