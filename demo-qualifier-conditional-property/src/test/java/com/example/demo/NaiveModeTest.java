package com.example.demo;

import com.example.demo.model.InventoryResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"demo.routing.strategy=naive", "demo.inventory.mode=mock"})
class NaiveModeTest {
    @Autowired
    private InventoryLookupUseCase useCase;

    @Test
    void shouldRouteInServiceLayerWhenNaiveStrategyIsEnabled() {
        InventoryResult result = useCase.lookup("SKU-1");
        assertThat(useCase.strategy()).isEqualTo("naive");
        assertThat(result.provider()).isEqualTo("local-mock-naive");
    }
}
