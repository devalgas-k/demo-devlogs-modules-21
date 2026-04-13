package com.example.demo.optimized;

import com.example.demo.InventoryLookupUseCase;
import com.example.demo.model.InventoryResult;
import com.example.demo.port.InventoryGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "demo.routing", name = "strategy", havingValue = "optimized", matchIfMissing = true)
public class OptimizedInventoryLookupService implements InventoryLookupUseCase {
    private final InventoryGateway inventoryGateway;

    public OptimizedInventoryLookupService(@Qualifier("inventoryGateway") InventoryGateway inventoryGateway) {
        this.inventoryGateway = inventoryGateway;
    }

    @Override
    public InventoryResult lookup(String sku) {
        return inventoryGateway.fetch(sku);
    }

    @Override
    public String strategy() {
        return "optimized";
    }
}
