package com.example.demo.beans.domain;

import org.springframework.stereotype.Component;

@Component
public final class AdyenGateway implements PaymentGateway {
    @Override
    public String name() {
        return "adyen";
    }
}
