package com.example.demo.beanslifecycle.domain;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;

public class ExpensiveBean {

    private static final AtomicInteger CONSTRUCTION_COUNT = new AtomicInteger();
    private static final AtomicInteger DESTROY_COUNT = new AtomicInteger();

    private final int instanceNumber;

    public ExpensiveBean() {
        this.instanceNumber = CONSTRUCTION_COUNT.incrementAndGet();
    }

    @PostConstruct
    public void init() {
        burnCpu(2_000_000);
    }

    @PreDestroy
    public void destroy() {
        DESTROY_COUNT.incrementAndGet();
    }

    public int instanceNumber() {
        return instanceNumber;
    }

    public long compute(long input) {
        long acc = input;
        for (int i = 0; i < 200_000; i++) {
            acc ^= (acc << 13);
            acc ^= (acc >>> 7);
            acc ^= (acc << 17);
        }
        return acc;
    }

    public static int constructionCount() {
        return CONSTRUCTION_COUNT.get();
    }

    public static int destroyCount() {
        return DESTROY_COUNT.get();
    }

    public static void resetCounters() {
        CONSTRUCTION_COUNT.set(0);
        DESTROY_COUNT.set(0);
    }

    private static void burnCpu(int iterations) {
        long acc = 0;
        for (int i = 0; i < iterations; i++) {
            acc ^= (i * 31L);
            acc = Long.rotateLeft(acc, 13);
        }
        if (acc == System.nanoTime()) {
            throw new IllegalStateException("Unreachable");
        }
    }
}
