package com.example.demo.beanslifecycle.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo")
public record DemoModeProperties(String mode) {
    public DemoModeProperties {
        if (mode == null || mode.isBlank()) {
            mode = "slow";
        }
    }
}
