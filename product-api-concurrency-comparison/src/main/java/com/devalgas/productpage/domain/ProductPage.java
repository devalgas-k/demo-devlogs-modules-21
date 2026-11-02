package com.devalgas.productpage.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductPage(
        Product product,
        Pricing pricing,
        Inventory inventory,
        Reviews reviews,
        Delivery delivery
) {
    public record Product(String id, String name, String description) {}

    public record Pricing(BigDecimal amount, String currency, String promotion) {}

    public record Inventory(boolean available, int quantity) {}

    public record Reviews(double average, int count) {}

    public record Delivery(LocalDate estimatedDate, String method) {}
}
