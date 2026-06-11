package com.example.demo.security.opaque;

import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CachingOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    private final OpaqueTokenIntrospector delegate;
    private final Duration ttl;
    private final int maxEntries;
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    public CachingOpaqueTokenIntrospector(OpaqueTokenIntrospector delegate, Duration ttl, int maxEntries) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.maxEntries = maxEntries;
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        Instant now = Instant.now();
        Entry cached = cache.get(token);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.principal();
        }

        OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);
        cache.put(token, new Entry(principal, now.plus(ttl)));
        if (cache.size() > maxEntries) {
            cache.keySet().stream().limit(Math.max(1, maxEntries / 10)).forEach(cache::remove);
        }
        return principal;
    }

    private record Entry(OAuth2AuthenticatedPrincipal principal, Instant expiresAt) {
    }
}
