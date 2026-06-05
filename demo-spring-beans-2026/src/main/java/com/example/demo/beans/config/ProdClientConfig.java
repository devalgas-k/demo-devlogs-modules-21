package com.example.demo.beans.config;

import com.example.demo.beans.domain.ExternalClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProdClientConfig {
    @Bean
    public ExternalClient externalClient() {
        return ExternalClient.http();
    }
}
