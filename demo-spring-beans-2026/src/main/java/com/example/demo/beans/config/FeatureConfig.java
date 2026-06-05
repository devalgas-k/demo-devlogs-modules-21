package com.example.demo.beans.config;

import com.example.demo.beans.domain.AuditService;
import com.example.demo.beans.domain.ClockProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FeatureConfig {
    @Bean
    @ConditionalOnProperty(name = "feature.audit.enabled", havingValue = "true")
    public AuditService auditService(ClockProvider clockProvider) {
        return new AuditService(clockProvider);
    }
}
