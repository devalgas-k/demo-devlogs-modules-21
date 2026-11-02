package com.devalgas.productpage.application;

import com.devalgas.productpage.domain.ProductPage;
import com.devalgas.productpage.simulation.DemoScenario;

public interface ProductPageOrchestrator {
    ProductPage aggregate(String productId, DemoScenario scenario);
}
