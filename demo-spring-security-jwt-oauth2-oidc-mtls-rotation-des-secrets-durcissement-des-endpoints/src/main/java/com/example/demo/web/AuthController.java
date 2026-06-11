package com.example.demo.web;

import com.example.demo.security.KeyRing;
import com.example.demo.security.SecurityStats;
import com.example.demo.security.jwt.DemoTokenService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

@RestController
public class AuthController {
    private final DemoTokenService tokenService;
    private final SecurityStats stats;

    public AuthController(DemoTokenService tokenService, SecurityStats stats) {
        this.tokenService = tokenService;
        this.stats = stats;
    }

    @GetMapping(path = "/auth/jwt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String jwt(
            @RequestParam(defaultValue = "alice") String sub,
            @RequestParam(defaultValue = "read") String scope,
            @RequestParam(defaultValue = "900") long ttlSeconds,
            @RequestParam(required = false) String iss,
            @RequestParam(required = false) String aud,
            @RequestParam(defaultValue = "0") long iatSecondsAgo,
            @RequestParam(defaultValue = "0") long nbfSecondsInFuture,
            @RequestParam(defaultValue = "false") boolean noKid
    ) {
        long ttl = Math.max(1, ttlSeconds);
        long iatAgo = Math.max(0, iatSecondsAgo);
        long nbfFuture = Math.max(0, nbfSecondsInFuture);
        return tokenService.mintJwt(
                sub,
                scope,
                Duration.ofSeconds(ttl),
                iss,
                aud,
                Duration.ofSeconds(-iatAgo),
                Duration.ofSeconds(nbfFuture),
                !noKid
        );
    }

    @GetMapping(path = "/auth/opaque", produces = MediaType.TEXT_PLAIN_VALUE)
    public String opaque(
            @RequestParam(defaultValue = "alice") String sub,
            @RequestParam(defaultValue = "read") String scope
    ) {
        return tokenService.mintOpaqueToken(sub, scope);
    }

    @PostMapping(path = "/auth/rotate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> rotate() {
        KeyRing.KeyEntry entry = tokenService.rotateKeys();
        return Map.of(
                "kid", entry.kid(),
                "createdAt", entry.createdAt().toString()
        );
    }

    @GetMapping(path = "/auth/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public SecurityStats.Snapshot stats() {
        return stats.snapshot();
    }
}
