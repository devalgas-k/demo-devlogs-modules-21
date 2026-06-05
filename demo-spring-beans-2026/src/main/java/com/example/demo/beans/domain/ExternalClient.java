package com.example.demo.beans.domain;

public final class ExternalClient {
    private final String mode;

    private ExternalClient(String mode) {
        this.mode = mode;
    }

    public static ExternalClient inMemory() {
        return new ExternalClient("in-memory");
    }

    public static ExternalClient http() {
        return new ExternalClient("http");
    }

    public String mode() {
        return mode;
    }
}
