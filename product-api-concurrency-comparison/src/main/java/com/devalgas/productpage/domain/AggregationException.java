package com.devalgas.productpage.domain;

public final class AggregationException extends RuntimeException {

    public enum Kind { UPSTREAM_FAILURE, GLOBAL_DEADLINE_EXCEEDED, INTERRUPTED }

    private final Kind kind;

    public AggregationException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static AggregationException upstream(Throwable cause) {
        return new AggregationException(Kind.UPSTREAM_FAILURE, "A required upstream service failed", cause);
    }

    public static AggregationException timeout(Throwable cause) {
        return new AggregationException(Kind.GLOBAL_DEADLINE_EXCEEDED, "The product page deadline was exceeded", cause);
    }

    public static AggregationException interrupted(InterruptedException cause) {
        return new AggregationException(Kind.INTERRUPTED, "Product page aggregation was interrupted", cause);
    }
}
