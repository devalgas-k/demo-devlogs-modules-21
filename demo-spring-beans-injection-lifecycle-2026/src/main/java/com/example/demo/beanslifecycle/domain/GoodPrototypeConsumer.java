package com.example.demo.beanslifecycle.domain;

import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class GoodPrototypeConsumer {

    private final ObjectProvider<PrototypeWidget> widgetProvider;

    public GoodPrototypeConsumer(ObjectProvider<PrototypeWidget> widgetProvider) {
        this.widgetProvider = widgetProvider;
    }

    public UUID newWidgetId() {
        return widgetProvider.getObject().id();
    }
}

