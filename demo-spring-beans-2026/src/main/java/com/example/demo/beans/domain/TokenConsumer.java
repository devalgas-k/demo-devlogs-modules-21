package com.example.demo.beans.domain;

import org.springframework.beans.factory.ObjectProvider;

public final class TokenConsumer {
    private final ObjectProvider<Token> tokenProvider;
    private final Token initialToken;

    public TokenConsumer(Token initialToken) {
        this.initialToken = initialToken;
        this.tokenProvider = null;
    }

    public TokenConsumer(ObjectProvider<Token> tokenProvider) {
        this.tokenProvider = tokenProvider;
        this.initialToken = null;
    }

    public Token newToken() {
        if (tokenProvider != null) {
            return tokenProvider.getObject();
        }
        return initialToken;
    }
}
