package com.example.demo.beans.config;

import com.example.demo.beans.domain.WidgetRepository;
import com.example.demo.beans.domain.WidgetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("slow")
public class SlowBeansConfig {

    @Bean
    public WidgetRepository widgetRepository() {
        return new WidgetRepository();
    }

    @Bean
    public WidgetService widgetService() {
        return new WidgetService(widgetRepository());
    }
}
