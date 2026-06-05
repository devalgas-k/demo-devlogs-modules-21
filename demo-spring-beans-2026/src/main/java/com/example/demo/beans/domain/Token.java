package com.example.demo.beans.domain;

import java.util.UUID;

public final class Token {
    private final UUID id = UUID.randomUUID();
    public UUID id() { return id; }
}
