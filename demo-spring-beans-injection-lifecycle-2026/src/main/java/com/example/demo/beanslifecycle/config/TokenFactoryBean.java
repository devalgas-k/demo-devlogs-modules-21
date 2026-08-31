package com.example.demo.beanslifecycle.config;

import com.example.demo.beanslifecycle.domain.Token;
import java.util.UUID;
import org.springframework.beans.factory.FactoryBean;

public class TokenFactoryBean implements FactoryBean<Token> {

    @Override
    public Token getObject() {
        return new Token(UUID.randomUUID().toString());
    }

    @Override
    public Class<?> getObjectType() {
        return Token.class;
    }
}

