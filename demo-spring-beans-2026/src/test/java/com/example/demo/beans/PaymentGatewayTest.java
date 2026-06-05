package com.example.demo.beans;

import com.example.demo.beans.domain.AdyenGateway;
import com.example.demo.beans.domain.CheckoutService;
import com.example.demo.beans.domain.PaymentGateway;
import com.example.demo.beans.domain.StripeGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("fast")
class PaymentGatewayTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    @Qualifier("adyenGateway")
    private PaymentGateway adyen;

    @Autowired
    @Qualifier("stripeGateway")
    private PaymentGateway stripe;

    @Test
    void shouldInjectStripeByDefaultBecauseOfPrimary() {
        // StripeGateway is @Primary
        assertThat(checkoutService.gatewayName()).isEqualTo("stripe");
    }

    @Test
    void shouldResolveByQualifier() {
        assertThat(adyen.name()).isEqualTo("adyen");
        assertThat(stripe.name()).isEqualTo("stripe");
    }

    @Test
    void shouldFailWhenAmbiguousWithoutQualifierOrPrimary() {
        // We simulate a scenario without @Primary and without @Qualifier
        // We use a manual context to avoid breaking the main app context
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.register(AdyenGatewayNoPrimary.class, StripeGatewayNoPrimary.class, CheckoutServiceNoQualifier.class);
            assertThatThrownBy(ctx::refresh)
                    .isInstanceOf(UnsatisfiedDependencyException.class);
        }
    }

    // Helper classes for the ambiguity test
    static class AdyenGatewayNoPrimary implements PaymentGateway {
        @Override public String name() { return "adyen"; }
    }

    static class StripeGatewayNoPrimary implements PaymentGateway {
        @Override public String name() { return "stripe"; }
    }

    @Service
    static class CheckoutServiceNoQualifier {
        public CheckoutServiceNoQualifier(PaymentGateway gateway) {}
    }
}
