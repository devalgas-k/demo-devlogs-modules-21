package com.example.demo.beanslifecycle.domain;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class BadPrototypeConsumer {

    private final PrototypeWidget widget;

    public BadPrototypeConsumer(PrototypeWidget widget) {
        this.widget = widget;
    }

    public UUID widgetId() {
        return widget.id();
    }
}

