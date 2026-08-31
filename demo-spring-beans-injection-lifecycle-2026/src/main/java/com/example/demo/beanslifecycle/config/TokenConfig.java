package com.example.demo.beanslifecycle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TokenConfig {

    @Bean(name = "demoToken")
    public TokenFactoryBean demoToken() {
        return new TokenFactoryBean();
    }
}

