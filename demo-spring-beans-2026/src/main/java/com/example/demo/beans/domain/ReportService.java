package com.example.demo.beans.domain;

import org.springframework.stereotype.Service;

@Service
public final class ReportService {
    private final WidgetService widgetService;

    public ReportService(WidgetService widgetService) {
        this.widgetService = widgetService;
    }

    public WidgetService widgetService() {
        return widgetService;
    }
}
