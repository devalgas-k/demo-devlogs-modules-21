package com.example.demo.beanslifecycle.config;

import com.example.demo.beanslifecycle.domain.ExpensiveBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "demo", name = "mode", havingValue = "fast")
public class FastModeConfig {

    @Bean
    @Lazy
    public ExpensiveBean expensiveBean() {
        return new ExpensiveBean();
    }
}
