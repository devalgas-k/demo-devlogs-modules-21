package com.example.demo.beans.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WidgetRepository {

    private final UUID instanceId = UUID.randomUUID();
    private final List<Widget> storage = new ArrayList<>();

    public UUID instanceId() {
        return instanceId;
    }

    public void save(Widget widget) {
        storage.add(widget);
    }

    public int count() {
        return storage.size();
    }

    public void clear() {
        storage.clear();
    }

    public record Widget(String name) {}
}
