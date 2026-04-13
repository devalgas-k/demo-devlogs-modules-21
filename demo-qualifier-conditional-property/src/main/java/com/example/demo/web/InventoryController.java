package com.example.demo.web;

import com.example.demo.InventoryLookupUseCase;
import com.example.demo.model.InventoryResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryLookupUseCase useCase;

    public InventoryController(InventoryLookupUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{sku}")
    public Map<String, Object> stock(@PathVariable String sku) {
        InventoryResult result = useCase.lookup(sku);
        return Map.of(
            "strategy", useCase.strategy(),
            "provider", result.provider(),
            "sku", result.sku(),
            "available", result.availableQuantity()
        );
    }
}
