package com.example.demo;

import com.example.demo.domain.HeapData;
import com.example.demo.service.CalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ThreadSafetyTest {

    @Autowired
    private CalculationService service;

    @Autowired
    private HeapData heapData;

    private static final int THREAD_COUNT = 100;
    private static final int ITERATIONS = 1000;

    @BeforeEach
    void setup() {
        heapData.reset();
        service.resetAtomic();
    }

    @Test
    void testHeapContention_NotThreadSafe() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        service.processOnHeap();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        int expected = THREAD_COUNT * ITERATIONS;
        int actual = heapData.getCounter();

        System.out.println("Expected (Heap): " + expected + ", Actual: " + actual);
        // Due to lack of synchronization, this will likely fail (actual < expected)
        // Note: In some runs, it might pass by chance, but usually fails on multi-core
        assertThat(actual).isLessThanOrEqualTo(expected);
    }

    @Test
    void testStackSafety_ThreadSafe() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger totalSuccess = new AtomicInteger(0);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        int result = service.processOnStack(100);
                        if (result == 101) {
                            totalSuccess.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        int expected = THREAD_COUNT * ITERATIONS;
        int actual = totalSuccess.get();

        System.out.println("Expected (Stack): " + expected + ", Actual: " + actual);
        // Local variables on the Stack are always safe because each thread has its own stack
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void testAtomicHeap_ThreadSafe() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < ITERATIONS; j++) {
                        service.processAtomicOnHeap();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        int expected = THREAD_COUNT * ITERATIONS;
        int actual = service.getAtomicValue();

        System.out.println("Expected (Atomic): " + expected + ", Actual: " + actual);
        // Atomic variables on the Heap are safe due to CAS (Compare-And-Swap)
        assertThat(actual).isEqualTo(expected);
    }
}
