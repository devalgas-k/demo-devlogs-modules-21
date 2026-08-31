package com.example.demo.beanslifecycle.config;

import com.example.demo.beanslifecycle.domain.ExpensiveBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "demo", name = "mode", havingValue = "slow", matchIfMissing = true)
public class SlowModeConfig {

    @Bean
    public ExpensiveBean expensiveBean() {
        return new ExpensiveBean();
    }
}
