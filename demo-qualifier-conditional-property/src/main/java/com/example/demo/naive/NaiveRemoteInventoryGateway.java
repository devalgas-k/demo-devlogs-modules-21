package com.example.demo.naive;

import com.example.demo.model.InventoryResult;
import com.example.demo.port.InventoryGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("naiveRemoteInventoryGateway")
@Qualifier("naiveRemoteInventoryGateway")
public class NaiveRemoteInventoryGateway implements InventoryGateway {
    @Override
    public InventoryResult fetch(String sku) {
        return new InventoryResult(sku, 42, "remote-api-naive");
    }
}
