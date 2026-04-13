package com.example.demo.optimized;

import com.example.demo.model.InventoryResult;
import com.example.demo.port.InventoryGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("remoteInventoryGateway")
@Qualifier("inventoryGateway")
@ConditionalOnProperty(prefix = "demo.inventory", name = "mode", havingValue = "remote", matchIfMissing = true)
public class RemoteInventoryGateway implements InventoryGateway {
    @Override
    public InventoryResult fetch(String sku) {
        return new InventoryResult(sku, 42, "remote-api");
    }
}
