package com.example.demo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "demo.security")
public record DemoSecurityProperties(
        String issuer,
        String audience,
        long introspectionDelayMs,
        long introspectionCacheTtlMs,
        long clockSkewSeconds
) {
}
