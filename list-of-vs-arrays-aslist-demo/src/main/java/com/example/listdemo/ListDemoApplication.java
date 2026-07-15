package com.example.listdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class ListDemoApplication {

    private static final Logger log = LoggerFactory.getLogger(ListDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ListDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner demoRunner(ListBehaviorDemo demo) {
        return args -> demo.runAllDemos();
    }

    @Component
    public static class ListBehaviorDemo {

        public void runAllDemos() {
            log.info("=".repeat(70));
            log.info("List.of() vs Arrays.asList() — Complete Behavior Demonstration");
            log.info("=".repeat(70));

            demonstrateArraysAsList();
            demonstrateListOf();
            demonstrateMutabilityDifferences();
            demonstrateNullHandling();
            demonstratePrimitiveArrayPitfall();
            demonstrateThreadSafety();
            demonstrateDefensiveCopy();

            log.info("=".repeat(70));
            log.info("All demonstrations completed successfully!");
            log.info("=".repeat(70));
        }

        private void demonstrateArraysAsList() {
            log.info("\n--- Arrays.asList() Demonstration ---");

            String[] array = {"A", "B", "C"};
            List<String> list = Arrays.asList(array);

            log.info("Original array: {}", Arrays.toString(array));
            log.info("Arrays.asList() result: {}", list);

            // set() works — modifies the backing array
            list.set(0, "Z");
            log.info("After list.set(0, 'Z'): list={}, array={}", list, Arrays.toString(array));

            // Modify via array — affects list
            array[1] = "Y";
            log.info("After array[1] = 'Y': list={}, array={}", list, Arrays.toString(array));

            // add() fails
            try {
                list.add("D");
                log.error("ERROR: add() should have thrown UnsupportedOperationException!");
            } catch (UnsupportedOperationException e) {
                log.info("list.add('D') correctly threw: {}", e.getClass().getSimpleName());
            }

            // remove() fails
            try {
                list.remove(0);
                log.error("ERROR: remove() should have thrown UnsupportedOperationException!");
            } catch (UnsupportedOperationException e) {
                log.info("list.remove(0) correctly threw: {}", e.getClass().getSimpleName());
            }
        }

        private void demonstrateListOf() {
            log.info("\n--- List.of() Demonstration ---");

            String[] array = {"A", "B", "C"};
            List<String> list = List.of(array);

            log.info("Original array: {}", Arrays.toString(array));
            log.info("List.of() result: {}", list);

            // Modify original array — does NOT affect List.of() result
            array[0] = "Z";
            log.info("After array[0] = 'Z': list={} (UNCHANGED!), array={}", list, Arrays.toString(array));

            // set() fails
            try {
                list.set(0, "Z");
                log.error("ERROR: set() should have thrown UnsupportedOperationException!");
            } catch (UnsupportedOperationException e) {
                log.info("list.set(0, 'Z') correctly threw: {}", e.getClass().getSimpleName());
            }

            // add() fails
            try {
                list.add("D");
                log.error("ERROR: add() should have thrown UnsupportedOperationException!");
            } catch (UnsupportedOperationException e) {
                log.info("list.add('D') correctly threw: {}", e.getClass().getSimpleName());
            }

            // remove() fails
            try {
                list.remove(0);
                log.error("ERROR: remove() should have thrown UnsupportedOperationException!");
            } catch (UnsupportedOperationException e) {
                log.info("list.remove(0) correctly threw: {}", e.getClass().getSimpleName());
            }
        }

        private void demonstrateMutabilityDifferences() {
            log.info("\n--- Mutability Matrix ---");
            log.info("| Method          | Arrays.asList() | List.of() |");
            log.info("|-----------------|------------------|-----------|");
            log.info("| get(index)      | ✓ WORKS          | ✓ WORKS   |");
            log.info("| set(index, val) | ✓ WORKS          | ✗ UOE     |");
            log.info("| add(element)    | ✗ UOE            | ✗ UOE     |");
            log.info("| remove(index)   | ✗ UOE            | ✗ UOE     |");
            log.info("| contains()      | ✓ WORKS          | ✓ WORKS   |");
            log.info("| size()          | ✓ WORKS          | ✓ WORKS   |");
        }

        private void demonstrateNullHandling() {
            log.info("\n--- Null Handling ---");

            // Arrays.asList() accepts null
            try {
                List<String> list1 = Arrays.asList("A", null, "C");
                log.info("Arrays.asList('A', null, 'C') = {} — null is allowed (but be careful!)", list1);
            } catch (Exception e) {
                log.error("Unexpected exception: {}", e.getClass().getSimpleName());
            }

            // List.of() rejects null
            try {
                List<String> list2 = List.of("A", null, "C");
                log.error("ERROR: List.of() should have rejected null!");
            } catch (NullPointerException e) {
                log.info("List.of('A', null, 'C') correctly threw: {} — null is NOT allowed", e.getClass().getSimpleName());
            }
        }

        private void demonstratePrimitiveArrayPitfall() {
            log.info("\n--- Primitive Array Pitfall ---");

            int[] primitives = {1, 2, 3};

            // WRONG: Creates List<int[]> with ONE element (the array itself)
            List<int[]> wrongList = Arrays.asList(primitives);
            log.warn("Arrays.asList(int[]) creates: {} with size={} (WRONG!)", wrongList, wrongList.size());

            // WRONG: Same problem with List.of()
            List<int[]> wrongList2 = List.of(primitives);
            log.warn("List.of(int[]) creates: {} with size={} (WRONG!)", wrongList2, wrongList2.size());

            // CORRECT: Box primitives first
            List<Integer> correctList = Arrays.stream(primitives).boxed().toList();
            log.info("Arrays.stream(int[]).boxed().toList() = {} with size={} (CORRECT!)", correctList, correctList.size());
        }

        private void demonstrateThreadSafety() {
            log.info("\n--- Thread Safety Considerations ---");
            log.info("Arrays.asList(): NOT thread-safe");
            log.info("  - The backing array can be modified from any thread");
            log.info("  - set() operations are visible across threads (data races!)");
            log.info("  - Never share Arrays$ArrayList between threads without synchronization");

            log.info("List.of(): Thread-safe (immutable)");
            log.info("  - No modifications possible, so no data races");
            log.info("  - Safe to share across threads without synchronization");
            log.info("  - Safe for use in concurrent code and Collections.unmodifiableMap()");
        }

        private void demonstrateDefensiveCopy() {
            log.info("\n--- Defensive Copy Patterns ---");

            String[] privateData = {"secret1", "secret2"};

            // BAD: Expose mutable list backed by internal array
            List<String> badExposal = Arrays.asList(privateData);
            log.warn("BAD: Arrays.asList() exposes internal array directly");
            badExposal.set(0, "HACKED");
            log.warn("  After badExposal.set(0, 'HACKED'): privateData={}", Arrays.toString(privateData));

            // RECOVERED: Reset for good demo
            privateData[0] = "secret1";

            // GOOD: List.of() creates independent copy
            List<String> goodExposal = List.of(privateData);
            log.info("GOOD: List.of() creates independent copy");
            privateData[0] = "HACKED";
            log.info("  After modifying privateData: goodExposal={} (UNCHANGED!)", goodExposal);
        }
    }
}
