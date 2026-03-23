package com.example.demo.domain;

import org.springframework.stereotype.Component;

/**
 * Shared state stored in the Heap.
 * Without synchronization, this is NOT thread-safe.
 */
@Component
public class HeapData {
    private int counter = 0;

    public void increment() {
        // Not atomic operation, classic thread-safety issue on the Heap
        counter++;
    }

    public int getCounter() {
        return counter;
    }

    public void reset() {
        counter = 0;
    }
}
