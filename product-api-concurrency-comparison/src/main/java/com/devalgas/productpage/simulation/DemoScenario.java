package com.devalgas.productpage.simulation;

public enum DemoScenario {
    SUCCESS,
    SLOW_SERVICE,
    ONE_FAILURE,
    MULTIPLE_FAILURES;

    public static DemoScenario parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unknown scenario: " + value, exception);
        }
    }
}
