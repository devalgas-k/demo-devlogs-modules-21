package com.example.demo.beans.domain;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public final class StripeGateway implements PaymentGateway {
    @Override
    public String name() {
        return "stripe";
    }
}
