package com.example.demo.beanslifecycle.config;

import com.example.demo.beanslifecycle.domain.ExpensiveBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "demo", name = "warmup", havingValue = "true")
public class WarmupRunner {

    @Bean
    public ApplicationRunner warmupExpensiveBean(ObjectProvider<ExpensiveBean> expensiveBeanProvider) {
        return args -> expensiveBeanProvider.getObject();
    }
}

