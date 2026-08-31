package com.example.demo.beanslifecycle.domain;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class FixedClockReportService {

    private final Clock fixedClock;

    public FixedClockReportService(@Qualifier("fixedClock") Clock fixedClock) {
        this.fixedClock = fixedClock;
    }

    public Instant now() {
        return Instant.now(fixedClock);
    }
}

