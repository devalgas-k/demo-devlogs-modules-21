package com.devalgas.productpage.simulation;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public final class CallTracker {

    private final AtomicInteger active = new AtomicInteger();
    private final ConcurrentHashMap<ServiceName, AtomicInteger> interrupted = new ConcurrentHashMap<>();

    public void started() {
        active.incrementAndGet();
    }

    public void interrupted(ServiceName service) {
        interrupted.computeIfAbsent(service, ignored -> new AtomicInteger()).incrementAndGet();
    }

    public void finished() {
        active.decrementAndGet();
    }

    public int activeCalls() {
        return active.get();
    }

    public int interruptedCalls() {
        return interrupted.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int interruptedCalls(ServiceName service) {
        AtomicInteger count = interrupted.get(service);
        return count == null ? 0 : count.get();
    }

    public void reset() {
        active.set(0);
        interrupted.clear();
    }
}
