package com.example.demo.beans.config;

import com.example.demo.beans.domain.ExpensiveBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration(proxyBeanMethods = false)
public class ExpensiveBeanConfig {

    @Bean
    @Lazy
    public ExpensiveBean expensiveBean() {
        return new ExpensiveBean();
    }
}
