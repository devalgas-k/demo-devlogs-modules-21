package com.devalgas.productpage.application;

import com.devalgas.productpage.domain.AggregationException;
import com.devalgas.productpage.domain.ProductPage;
import com.devalgas.productpage.simulation.DemoScenario;
import com.devalgas.productpage.simulation.SimulatedProductApis;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Service("completableFutureOrchestrator")
public final class CompletableFutureProductPageOrchestrator implements ProductPageOrchestrator {

    private final SimulatedProductApis apis;
    private final Duration timeout;
    private final ThreadFactory threadFactory = Thread.ofVirtual().name("product-api-", 0).factory();

    public CompletableFutureProductPageOrchestrator(
            SimulatedProductApis apis,
            @Qualifier("aggregationTimeout") Duration timeout
    ) {
        this.apis = apis;
        this.timeout = timeout;
    }

    @Override
    public ProductPage aggregate(String productId, DemoScenario scenario) {
        long deadlineNanos = System.nanoTime() + timeout.toNanos();
        StartedTask<ProductPage.Product> product = start(() -> apis.product(productId, scenario));
        StartedTask<ProductPage.Pricing> pricing = start(() -> apis.pricing(scenario));
        StartedTask<ProductPage.Inventory> inventory = start(() -> apis.inventory(scenario));
        StartedTask<ProductPage.Reviews> reviews = start(() -> apis.reviews(scenario));
        StartedTask<ProductPage.Delivery> delivery = start(() -> apis.delivery(scenario));
        List<StartedTask<?>> tasks = List.of(product, pricing, inventory, reviews, delivery);
        try {
            tasks.forEach(StartedTask::start);
        } catch (RuntimeException | Error startFailure) {
            cancelAndAwait(tasks);
            throw startFailure;
        }

        CompletableFuture<Void> outcome = failFastOutcome(tasks);
        try {
            long remainingNanos = Math.max(0, deadlineNanos - System.nanoTime());
            outcome.get(remainingNanos, TimeUnit.NANOSECONDS);
            return new ProductPage(
                    product.result().join(), pricing.result().join(), inventory.result().join(),
                    reviews.result().join(), delivery.result().join());
        } catch (TimeoutException timeoutException) {
            cancelAndAwait(tasks);
            throw AggregationException.timeout(timeoutException);
        } catch (ExecutionException executionException) {
            cancelAndAwait(tasks);
            throw AggregationException.upstream(rootCause(executionException));
        } catch (InterruptedException interruptedException) {
            cancelAndAwait(tasks);
            Thread.currentThread().interrupt();
            throw AggregationException.interrupted(interruptedException);
        }
    }

    private CompletableFuture<Void> failFastOutcome(List<StartedTask<?>> tasks) {
        var outcome = new CompletableFuture<Void>();
        var successes = new AtomicInteger();
        tasks.forEach(task -> task.result().whenComplete((ignored, failure) -> {
            if (failure != null) {
                outcome.completeExceptionally(failure);
            } else if (successes.incrementAndGet() == tasks.size()) {
                outcome.complete(null);
            }
        }));
        return outcome;
    }

    private <T> StartedTask<T> start(InterruptibleSupplier<T> supplier) {
        var result = new CompletableFuture<T>();
        Thread thread = threadFactory.newThread(() -> {
            try {
                result.complete(supplier.get());
            } catch (Exception failure) {
                result.completeExceptionally(failure);
            }
        });
        return new StartedTask<>(result, thread);
    }

    private void cancelAndAwait(List<StartedTask<?>> tasks) {
        tasks.stream().map(StartedTask::thread).filter(Thread::isAlive).forEach(Thread::interrupt);
        boolean interrupted = false;
        for (StartedTask<?> task : tasks) {
            while (task.thread().isAlive()) {
                try {
                    task.thread().join();
                } catch (InterruptedException cleanupInterrupted) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private Throwable rootCause(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof ExecutionException || current instanceof java.util.concurrent.CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private record StartedTask<T>(CompletableFuture<T> result, Thread thread) {
        void start() {
            thread.start();
        }
    }

    @FunctionalInterface
    private interface InterruptibleSupplier<T> {
        T get() throws Exception;
    }
}
