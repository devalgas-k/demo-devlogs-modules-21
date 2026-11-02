package com.devalgas.productpage.simulation;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

public final class ScenarioPlan {

    public record Behavior(Duration latency, boolean fails) {}

    private ScenarioPlan() {}

    public static Map<ServiceName, Behavior> forScenario(DemoScenario scenario) {
        var plan = successPlan();
        switch (scenario) {
            case SUCCESS -> { }
            case SLOW_SERVICE -> plan.put(ServiceName.DELIVERY, behavior(900, false));
            case ONE_FAILURE -> {
                plan.put(ServiceName.PRODUCT, behavior(800, false));
                plan.put(ServiceName.PRICING, behavior(60, true));
                plan.put(ServiceName.INVENTORY, behavior(800, false));
                plan.put(ServiceName.REVIEWS, behavior(800, false));
                plan.put(ServiceName.DELIVERY, behavior(800, false));
            }
            case MULTIPLE_FAILURES -> {
                plan.put(ServiceName.PRICING, behavior(70, true));
                plan.put(ServiceName.REVIEWS, behavior(70, true));
                plan.put(ServiceName.DELIVERY, behavior(800, false));
            }
        }
        return Map.copyOf(plan);
    }

    private static EnumMap<ServiceName, Behavior> successPlan() {
        var plan = new EnumMap<ServiceName, Behavior>(ServiceName.class);
        plan.put(ServiceName.PRODUCT, behavior(80, false));
        plan.put(ServiceName.PRICING, behavior(100, false));
        plan.put(ServiceName.INVENTORY, behavior(120, false));
        plan.put(ServiceName.REVIEWS, behavior(140, false));
        plan.put(ServiceName.DELIVERY, behavior(160, false));
        return plan;
    }

    private static Behavior behavior(long milliseconds, boolean fails) {
        return new Behavior(Duration.ofMillis(milliseconds), fails);
    }
}
