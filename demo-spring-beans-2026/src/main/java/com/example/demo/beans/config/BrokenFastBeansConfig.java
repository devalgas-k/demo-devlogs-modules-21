package com.example.demo.beans.config;

import com.example.demo.beans.domain.WidgetRepository;
import com.example.demo.beans.domain.WidgetService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("broken")
public class BrokenFastBeansConfig {

    @Bean
    public WidgetRepository widgetRepository() {
        return new WidgetRepository();
    }

    @Bean
    public WidgetService widgetService() {
        return new WidgetService(widgetRepository());
    }
}
