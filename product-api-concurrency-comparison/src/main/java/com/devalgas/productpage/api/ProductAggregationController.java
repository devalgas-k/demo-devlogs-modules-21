package com.devalgas.productpage.api;

import com.devalgas.productpage.application.ProductPageOrchestrator;
import com.devalgas.productpage.domain.ProductPage;
import com.devalgas.productpage.simulation.DemoScenario;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductAggregationController {

    private final ProductPageOrchestrator completableFuture;
    private final ProductPageOrchestrator structuredConcurrency;

    public ProductAggregationController(
            @Qualifier("completableFutureOrchestrator") ProductPageOrchestrator completableFuture,
            @Qualifier("structuredConcurrencyOrchestrator") ProductPageOrchestrator structuredConcurrency
    ) {
        this.completableFuture = completableFuture;
        this.structuredConcurrency = structuredConcurrency;
    }

    @GetMapping("/{id}/completable-future")
    ResponseEntity<ProductPage> completableFuture(
            @PathVariable String id,
            @RequestParam(defaultValue = "SUCCESS") String scenario
    ) {
        return response(completableFuture, id, scenario, "completable-future");
    }

    @GetMapping("/{id}/structured-concurrency")
    ResponseEntity<ProductPage> structuredConcurrency(
            @PathVariable String id,
            @RequestParam(defaultValue = "SUCCESS") String scenario
    ) {
        return response(structuredConcurrency, id, scenario, "structured-concurrency");
    }

    private ResponseEntity<ProductPage> response(
            ProductPageOrchestrator orchestrator, String id, String scenario, String strategy
    ) {
        if (id.isBlank()) {
            throw new IllegalArgumentException("Product id must not be blank");
        }
        return ResponseEntity.ok()
                .header("X-Orchestration-Strategy", strategy)
                .body(orchestrator.aggregate(id, DemoScenario.parse(scenario)));
    }
}
