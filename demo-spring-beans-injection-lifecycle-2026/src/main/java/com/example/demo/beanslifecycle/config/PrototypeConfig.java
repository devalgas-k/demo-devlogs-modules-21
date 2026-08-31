package com.example.demo.beanslifecycle.config;

import com.example.demo.beanslifecycle.domain.PrototypeWidget;
import java.util.UUID;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration(proxyBeanMethods = false)
public class PrototypeConfig {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public PrototypeWidget prototypeWidget() {
        return new PrototypeWidget(UUID.randomUUID());
    }
}

