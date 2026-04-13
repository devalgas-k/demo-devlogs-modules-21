package com.example.demo.naive;

import com.example.demo.model.InventoryResult;
import com.example.demo.port.InventoryGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("naiveLocalInventoryGateway")
@Qualifier("naiveLocalInventoryGateway")
public class NaiveLocalInventoryGateway implements InventoryGateway {
    @Override
    public InventoryResult fetch(String sku) {
        return new InventoryResult(sku, 999, "local-mock-naive");
    }
}
