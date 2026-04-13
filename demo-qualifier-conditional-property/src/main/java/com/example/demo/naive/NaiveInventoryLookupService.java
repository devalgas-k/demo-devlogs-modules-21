package com.example.demo.naive;

import com.example.demo.InventoryLookupUseCase;
import com.example.demo.model.InventoryResult;
import com.example.demo.port.InventoryGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "demo.routing", name = "strategy", havingValue = "naive")
public class NaiveInventoryLookupService implements InventoryLookupUseCase {
    private final InventoryGateway remoteGateway;
    private final InventoryGateway localGateway;
    private final String mode;

    public NaiveInventoryLookupService(
        @Qualifier("naiveRemoteInventoryGateway") InventoryGateway remoteGateway,
        @Qualifier("naiveLocalInventoryGateway") InventoryGateway localGateway,
        @Value("${demo.inventory.mode:remote}") String mode
    ) {
        this.remoteGateway = remoteGateway;
        this.localGateway = localGateway;
        this.mode = mode;
    }

    @Override
    public InventoryResult lookup(String sku) {
        return "mock".equalsIgnoreCase(mode) ? localGateway.fetch(sku) : remoteGateway.fetch(sku);
    }

    @Override
    public String strategy() {
        return "naive";
    }
}
