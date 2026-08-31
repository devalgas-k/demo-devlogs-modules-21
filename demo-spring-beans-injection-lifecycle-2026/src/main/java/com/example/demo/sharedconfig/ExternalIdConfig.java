package com.example.demo.sharedconfig;

import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ExternalIdConfig {

    @Bean
    public ImportedIdGenerator importedIdGenerator() {
        return () -> "imported-" + UUID.randomUUID();
    }

    public interface ImportedIdGenerator {
        String newId();
    }
}

