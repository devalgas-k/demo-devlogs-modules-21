package com.example.demo.port;

import com.example.demo.model.InventoryResult;

public interface InventoryGateway {
    InventoryResult fetch(String sku);
}
