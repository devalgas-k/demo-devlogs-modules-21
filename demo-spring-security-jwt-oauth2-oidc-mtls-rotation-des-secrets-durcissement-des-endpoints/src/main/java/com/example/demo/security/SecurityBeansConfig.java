package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeansConfig {
    @Bean
    KeyRing keyRing() {
        return new KeyRing();
    }

    @Bean
    SecurityStats securityStats() {
        return new SecurityStats();
    }
}
