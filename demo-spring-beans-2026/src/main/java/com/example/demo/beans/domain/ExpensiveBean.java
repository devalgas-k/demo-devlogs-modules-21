package com.example.demo.beans.domain;

import java.time.Instant;
import java.util.UUID;

public final class ExpensiveBean {

    private final UUID instanceId = UUID.randomUUID();
    private final Instant createdAt = Instant.now();

    public ExpensiveBean() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating ExpensiveBean", e);
        }
    }

    public UUID instanceId() {
        return instanceId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
