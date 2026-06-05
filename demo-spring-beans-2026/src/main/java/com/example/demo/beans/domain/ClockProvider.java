package com.example.demo.beans.domain;

import org.springframework.stereotype.Component;

@Component
public final class ClockProvider {
    public long nowEpochMillis() {
        return System.currentTimeMillis();
    }
}
