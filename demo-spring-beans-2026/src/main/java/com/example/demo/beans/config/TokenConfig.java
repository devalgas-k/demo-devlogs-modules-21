package com.example.demo.beans.config;

import com.example.demo.beans.domain.Token;
import com.example.demo.beans.domain.TokenConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration(proxyBeanMethods = false)
public class TokenConfig {
    @Bean
    @Scope("prototype")
    public Token token() {
        return new Token();
    }

    @Bean
    public TokenConsumer tokenConsumer(org.springframework.beans.factory.ObjectProvider<Token> tokenProvider) {
        return new TokenConsumer(tokenProvider);
    }
}
