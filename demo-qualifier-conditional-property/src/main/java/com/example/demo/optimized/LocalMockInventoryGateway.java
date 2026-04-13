package com.example.demo.optimized;

import com.example.demo.model.InventoryResult;
import com.example.demo.port.InventoryGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("localInventoryGateway")
@Qualifier("inventoryGateway")
@ConditionalOnProperty(prefix = "demo.inventory", name = "mode", havingValue = "mock")
public class LocalMockInventoryGateway implements InventoryGateway {
    @Override
    public InventoryResult fetch(String sku) {
        return new InventoryResult(sku, 999, "local-mock");
    }
}
