package com.devalgas.productpage.application;

import com.devalgas.productpage.domain.AggregationException;
import com.devalgas.productpage.simulation.CallTracker;
import com.devalgas.productpage.simulation.DemoScenario;
import com.devalgas.productpage.simulation.SimulatedProductApis;
import com.devalgas.productpage.simulation.ServiceName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrchestratorBehaviorTest {

    private final CallTracker tracker = new CallTracker();
    private final SimulatedProductApis apis = new SimulatedProductApis(tracker);
    private final Duration timeout = Duration.ofMillis(500);
    private final ProductPageOrchestrator completableFuture =
            new CompletableFutureProductPageOrchestrator(apis, timeout);
    private final ProductPageOrchestrator structuredConcurrency =
            new StructuredConcurrencyProductPageOrchestrator(apis, timeout);

    @Test
    void bothStrategiesProduceTheSameSuccessfulContract() {
        var completableResult = completableFuture.aggregate("keyboard-42", DemoScenario.SUCCESS);
        var structuredResult = structuredConcurrency.aggregate("keyboard-42", DemoScenario.SUCCESS);

        assertThat(completableResult).isEqualTo(structuredResult);
        assertThat(tracker.activeCalls()).isZero();
    }

    @Test
    void bothStrategiesFailFastAndCleanUpSiblings() {
        for (ProductPageOrchestrator orchestrator : orchestrators()) {
            tracker.reset();
            assertThatThrownBy(() -> orchestrator.aggregate("keyboard-42", DemoScenario.ONE_FAILURE))
                    .isInstanceOfSatisfying(AggregationException.class,
                            failure -> assertThat(failure.kind()).isEqualTo(AggregationException.Kind.UPSTREAM_FAILURE));
            assertThat(tracker.activeCalls()).isZero();
            assertThat(tracker.interruptedCalls(ServiceName.PRODUCT)).isEqualTo(1);
            assertThat(tracker.interruptedCalls(ServiceName.INVENTORY)).isEqualTo(1);
            assertThat(tracker.interruptedCalls(ServiceName.REVIEWS)).isEqualTo(1);
            assertThat(tracker.interruptedCalls(ServiceName.DELIVERY)).isEqualTo(1);
        }
    }

    @Test
    void bothStrategiesEnforceOneGlobalDeadlineAndCleanUp() {
        for (ProductPageOrchestrator orchestrator : orchestrators()) {
            tracker.reset();
            assertThatThrownBy(() -> orchestrator.aggregate("keyboard-42", DemoScenario.SLOW_SERVICE))
                    .isInstanceOfSatisfying(AggregationException.class,
                            failure -> assertThat(failure.kind()).isEqualTo(
                                    AggregationException.Kind.GLOBAL_DEADLINE_EXCEEDED));
            assertThat(tracker.activeCalls()).isZero();
            assertThat(tracker.interruptedCalls(ServiceName.DELIVERY)).isEqualTo(1);
            assertThat(tracker.interruptedCalls()).isEqualTo(1);
        }
    }

    @Test
    void concurrentFailuresStillReturnAStableBusinessError() {
        for (ProductPageOrchestrator orchestrator : orchestrators()) {
            tracker.reset();
            assertThatThrownBy(() -> orchestrator.aggregate("keyboard-42", DemoScenario.MULTIPLE_FAILURES))
                    .isInstanceOfSatisfying(AggregationException.class,
                            failure -> assertThat(failure.kind()).isEqualTo(AggregationException.Kind.UPSTREAM_FAILURE));
            assertThat(tracker.activeCalls()).isZero();
        }
    }

    @Test
    void interruptingTheOwnerInterruptsChildrenAndRestoresTheFlag() throws Exception {
        for (ProductPageOrchestrator orchestrator : orchestrators()) {
            tracker.reset();
            var failure = new AtomicReference<Throwable>();
            var interruptRestored = new AtomicBoolean();
            Thread owner = Thread.ofPlatform().start(() -> {
                try {
                    orchestrator.aggregate("keyboard-42", DemoScenario.ONE_FAILURE);
                } catch (Throwable throwable) {
                    failure.set(throwable);
                    interruptRestored.set(Thread.currentThread().isInterrupted());
                }
            });

            awaitFiveActiveCalls();
            owner.interrupt();
            owner.join(2_000);

            assertThat(owner.isAlive()).isFalse();
            assertThat(failure.get()).isInstanceOfSatisfying(AggregationException.class,
                    exception -> assertThat(exception.kind()).isEqualTo(AggregationException.Kind.INTERRUPTED));
            assertThat(interruptRestored).isTrue();
            assertThat(tracker.activeCalls()).isZero();
            assertThat(tracker.interruptedCalls()).isGreaterThanOrEqualTo(1);
        }
    }

    private List<ProductPageOrchestrator> orchestrators() {
        return List.of(completableFuture, structuredConcurrency);
    }

    private void awaitFiveActiveCalls() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(1).toNanos();
        while (tracker.activeCalls() < 5 && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
        assertThat(tracker.activeCalls()).isEqualTo(5);
    }
}
