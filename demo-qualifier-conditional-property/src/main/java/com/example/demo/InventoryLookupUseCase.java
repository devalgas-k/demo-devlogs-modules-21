package com.example.demo;

import com.example.demo.model.InventoryResult;

public interface InventoryLookupUseCase {
    InventoryResult lookup(String sku);
    String strategy();
}
