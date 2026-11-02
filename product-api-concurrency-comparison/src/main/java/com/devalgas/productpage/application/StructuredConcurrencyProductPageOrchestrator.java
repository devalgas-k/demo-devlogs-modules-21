package com.devalgas.productpage.application;

import com.devalgas.productpage.domain.AggregationException;
import com.devalgas.productpage.domain.ProductPage;
import com.devalgas.productpage.simulation.DemoScenario;
import com.devalgas.productpage.simulation.SimulatedProductApis;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.StructuredTaskScope;

@Service("structuredConcurrencyOrchestrator")
public final class StructuredConcurrencyProductPageOrchestrator implements ProductPageOrchestrator {

    private final SimulatedProductApis apis;
    private final Duration timeout;

    public StructuredConcurrencyProductPageOrchestrator(
            SimulatedProductApis apis,
            @Qualifier("aggregationTimeout") Duration timeout
    ) {
        this.apis = apis;
        this.timeout = timeout;
    }

    @Override
    public ProductPage aggregate(String productId, DemoScenario scenario) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        try (var scope = StructuredTaskScope.<Object, Void>open(
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow(),
                configuration -> configuration.withName("product-page").withTimeout(
                        Duration.ofNanos(Math.max(1, deadlineNanos - System.nanoTime()))))) {
            var product = scope.fork(() -> apis.product(productId, scenario));
            var pricing = scope.fork(() -> apis.pricing(scenario));
            var inventory = scope.fork(() -> apis.inventory(scenario));
            var reviews = scope.fork(() -> apis.reviews(scenario));
            var delivery = scope.fork(() -> apis.delivery(scenario));

            scope.join();
            return new ProductPage(product.get(), pricing.get(), inventory.get(), reviews.get(), delivery.get());
        } catch (StructuredTaskScope.TimeoutException timeoutException) {
            throw AggregationException.timeout(timeoutException);
        } catch (StructuredTaskScope.FailedException failedException) {
            throw AggregationException.upstream(failedException.getCause());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw AggregationException.interrupted(interruptedException);
        }
    }
}
