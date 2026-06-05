package com.example.demo.beans.config;

import com.example.demo.beans.domain.WidgetRepository;
import com.example.demo.beans.domain.WidgetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("fast")
public class FastBeansConfig {

    @Bean
    public WidgetRepository widgetRepository() {
        return new WidgetRepository();
    }

    @Bean
    public WidgetService widgetService(WidgetRepository widgetRepository) {
        return new WidgetService(widgetRepository);
    }
}
