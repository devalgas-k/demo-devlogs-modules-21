package com.example.demo.beanslifecycle;

import com.example.demo.beanslifecycle.config.DemoModeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(DemoModeProperties.class)
public class DemoSpringBeansInjectionLifecycle2026Application {

    public static void main(String[] args) {
        SpringApplication.run(DemoSpringBeansInjectionLifecycle2026Application.class, args);
    }
}

