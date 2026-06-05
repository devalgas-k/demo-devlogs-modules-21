package com.example.demo.beans.domain;

import java.util.UUID;

public final class WidgetRepository {

    private final UUID instanceId = UUID.randomUUID();

    public UUID instanceId() {
        return instanceId;
    }
}
