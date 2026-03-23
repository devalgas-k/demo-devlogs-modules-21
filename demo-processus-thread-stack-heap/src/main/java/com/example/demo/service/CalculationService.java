package com.example.demo.service;

import com.example.demo.domain.HeapData;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service demonstrating the difference between Stack (local) and Heap (shared) memory.
 */
@Service
public class CalculationService {

    private final HeapData sharedHeapData;

    public CalculationService(HeapData sharedHeapData) {
        this.sharedHeapData = sharedHeapData;
    }

    /**
     * Non-optimized: Uses shared state in the Heap.
     * Dangerous for multi-threading without explicit synchronization.
     */
    public void processOnHeap() {
        sharedHeapData.increment();
    }

    /**
     * Optimized/Safe: Uses local variables on the Stack.
     * Each thread has its own Stack, making this inherently thread-safe.
     * @return the result of the local calculation
     */
    public int processOnStack(int input) {
        // 'localValue' is stored on the thread's Stack
        int localValue = input;
        localValue += 1;
        return localValue;
    }

    /**
     * Thread-safe Heap usage using Atomic variables.
     * Still on the Heap, but using hardware-level synchronization (CAS).
     */
    private final AtomicInteger atomicCounter = new AtomicInteger(0);

    public void processAtomicOnHeap() {
        atomicCounter.incrementAndGet();
    }

    public int getAtomicValue() {
        return atomicCounter.get();
    }

    public void resetAtomic() {
        atomicCounter.set(0);
    }
}
