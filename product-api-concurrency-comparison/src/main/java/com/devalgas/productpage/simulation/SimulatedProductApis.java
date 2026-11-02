package com.devalgas.productpage.simulation;

import com.devalgas.productpage.domain.ProductPage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public final class SimulatedProductApis {

    private final CallTracker tracker;

    public SimulatedProductApis(CallTracker tracker) {
        this.tracker = tracker;
    }

    public ProductPage.Product product(String id, DemoScenario scenario) throws InterruptedException {
        return call(ServiceName.PRODUCT, scenario,
                new ProductPage.Product(id, "Mechanical keyboard", "A compact hot-swappable keyboard"));
    }

    public ProductPage.Pricing pricing(DemoScenario scenario) throws InterruptedException {
        return call(ServiceName.PRICING, scenario,
                new ProductPage.Pricing(new BigDecimal("129.90"), "EUR", "SPRING10"));
    }

    public ProductPage.Inventory inventory(DemoScenario scenario) throws InterruptedException {
        return call(ServiceName.INVENTORY, scenario, new ProductPage.Inventory(true, 42));
    }

    public ProductPage.Reviews reviews(DemoScenario scenario) throws InterruptedException {
        return call(ServiceName.REVIEWS, scenario, new ProductPage.Reviews(4.7, 318));
    }

    public ProductPage.Delivery delivery(DemoScenario scenario) throws InterruptedException {
        return call(ServiceName.DELIVERY, scenario,
                new ProductPage.Delivery(LocalDate.of(2026, 11, 5), "STANDARD"));
    }

    private <T> T call(ServiceName service, DemoScenario scenario, T value) throws InterruptedException {
        Map<ServiceName, ScenarioPlan.Behavior> plan = ScenarioPlan.forScenario(scenario);
        ScenarioPlan.Behavior behavior = plan.get(service);
        tracker.started();
        try {
            Thread.sleep(behavior.latency());
            if (behavior.fails()) {
                throw new UpstreamServiceException(service);
            }
            return value;
        } catch (InterruptedException interrupted) {
            tracker.interrupted(service);
            throw interrupted;
        } finally {
            tracker.finished();
        }
    }
}
