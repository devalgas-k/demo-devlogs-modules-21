package com.example.demo.beans.domain;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public final class CheckoutService {
    private final PaymentGateway paymentGateway;

    public CheckoutService(@Qualifier("stripeGateway") PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public String gatewayName() {
        return paymentGateway.name();
    }
}
