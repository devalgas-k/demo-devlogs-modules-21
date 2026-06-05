package com.example.demo.beans.domain;

public final class WidgetService {

    private final WidgetRepository widgetRepository;

    public WidgetService(WidgetRepository widgetRepository) {
        this.widgetRepository = widgetRepository;
    }

    public WidgetRepository widgetRepository() {
        return widgetRepository;
    }
}
