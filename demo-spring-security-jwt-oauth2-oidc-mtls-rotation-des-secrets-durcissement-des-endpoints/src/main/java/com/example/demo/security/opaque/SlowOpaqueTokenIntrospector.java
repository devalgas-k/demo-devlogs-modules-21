package com.example.demo.security.opaque;

import com.example.demo.security.SecurityStats;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SlowOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    private final Duration delay;
    private final SecurityStats stats;

    public SlowOpaqueTokenIntrospector(Duration delay, SecurityStats stats) {
        this.delay = delay;
        this.stats = stats;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        Instant start = Instant.now();
        try {
            sleep(delay);
            return principalFromOpaqueToken(token);
        } catch (RuntimeException e) {
            throw new OAuth2IntrospectionException("Invalid opaque token", e);
        } finally {
            stats.onOpaqueIntrospection(Duration.between(start, Instant.now()));
        }
    }

    private static OAuth2AuthenticatedPrincipal principalFromOpaqueToken(String token) {
        String[] parts = token.split("\\.", 3);
        if (parts.length != 3 || !parts[0].equals("opaque")) {
            throw new IllegalArgumentException("Bad token format");
        }

        String subject = parts[1];
        String scope = parts[2];

        List<GrantedAuthority> authorities = scope.isBlank()
                ? List.of()
                : Stream.of(scope.split("\\s+"))
                .filter(s -> !s.isBlank())
                .distinct()
                .map(s -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + s))
                .toList();

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", subject);
        attributes.put("scope", scope);

        return new DefaultOAuth2AuthenticatedPrincipal(subject, attributes, authorities);
    }

    private static void sleep(Duration delay) {
        if (delay.isZero() || delay.isNegative()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }
}
