package com.example.demo;

import com.example.demo.model.InventoryResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"demo.routing.strategy=optimized", "demo.inventory.mode=mock"})
class OptimizedMockModeTest {
    @Autowired
    private InventoryLookupUseCase useCase;

    @Test
    void shouldUseLocalMockProviderWhenModeIsMock() {
        InventoryResult result = useCase.lookup("SKU-1");
        assertThat(useCase.strategy()).isEqualTo("optimized");
        assertThat(result.provider()).isEqualTo("local-mock");
    }
}
