package com.devalgas.productpage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
@Configuration
public class ConcurrencyConfig {

    @Bean
    Duration aggregationTimeout(@Value("${demo.aggregation.timeout:500ms}") Duration timeout) {
        return timeout;
    }
}
