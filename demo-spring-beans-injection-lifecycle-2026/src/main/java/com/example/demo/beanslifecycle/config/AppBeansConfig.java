package com.example.demo.beanslifecycle.config;

import com.example.demo.sharedconfig.ExternalIdConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(ExternalIdConfig.class)
public class AppBeansConfig {
}

