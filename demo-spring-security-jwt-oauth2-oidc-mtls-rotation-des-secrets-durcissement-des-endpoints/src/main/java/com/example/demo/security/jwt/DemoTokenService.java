package com.example.demo.security.jwt;

import com.example.demo.security.DemoSecurityProperties;
import com.example.demo.security.KeyRing;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class DemoTokenService {
    private final DemoSecurityProperties properties;
    private final KeyRing keyRing;

    public DemoTokenService(DemoSecurityProperties properties, KeyRing keyRing) {
        this.properties = properties;
        this.keyRing = keyRing;
    }

    public String mintJwt(String subject, String scope, Duration ttl) {
        return mintJwt(subject, scope, ttl, null, null, Duration.ZERO, Duration.ZERO, true);
    }

    public String mintJwt(String subject, String scope, Duration ttl, String issuerOverride, String audienceOverride, Duration issuedAtOffset) {
        return mintJwt(subject, scope, ttl, issuerOverride, audienceOverride, issuedAtOffset, Duration.ZERO, true);
    }

    public String mintJwt(
            String subject,
            String scope,
            Duration ttl,
            String issuerOverride,
            String audienceOverride,
            Duration issuedAtOffset,
            Duration notBeforeInFuture,
            boolean includeKid
    ) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(ttl, "ttl");
        Objects.requireNonNull(issuedAtOffset, "issuedAtOffset");
        Objects.requireNonNull(notBeforeInFuture, "notBeforeInFuture");
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }

        KeyRing.KeyEntry current = keyRing.current();
        Instant now = Instant.now();
        Instant issuedAt = now.plus(issuedAtOffset);
        Instant exp = issuedAt.plus(ttl);
        Instant nbf = now.plus(notBeforeInFuture);

        String issuer = issuerOverride == null ? properties.issuer() : issuerOverride;
        String audience = audienceOverride == null ? properties.audience() : audienceOverride;

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .subject(subject)
                .issuer(issuer)
                .audience(audience)
                .issueTime(java.util.Date.from(issuedAt))
                .expirationTime(java.util.Date.from(exp))
                .claim("scope", scope);

        if (!notBeforeInFuture.isZero() && !notBeforeInFuture.isNegative()) {
            builder = builder.notBeforeTime(java.util.Date.from(nbf));
        }

        JWTClaimsSet claims = builder.build();

        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT);
        if (includeKid) {
            headerBuilder = headerBuilder.keyID(current.kid());
        }
        JWSHeader header = headerBuilder.build();

        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new RSASSASigner(current.privateKey()));
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
        return jwt.serialize();
    }

    public String mintOpaqueToken(String subject, String scope) {
        return "opaque." + subject + "." + scope;
    }

    public KeyRing.KeyEntry rotateKeys() {
        return keyRing.rotate();
    }
}
