package com.example.demo.security.jwt;

import com.example.demo.security.KeyRing;
import com.example.demo.security.SecurityStats;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RotatingRsaJwtDecoder implements JwtDecoder {
    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error("invalid_token", "Invalid token", null);

    private final KeyRing keyRing;
    private final SecurityStats stats;

    private volatile OAuth2TokenValidator<Jwt> jwtValidator = jwt -> OAuth2TokenValidatorResult.success();
    private volatile String audience;

    public RotatingRsaJwtDecoder(KeyRing keyRing, SecurityStats stats) {
        this.keyRing = keyRing;
        this.stats = stats;
    }

    public void setJwtValidator(OAuth2TokenValidator<Jwt> jwtValidator) {
        this.jwtValidator = Objects.requireNonNull(jwtValidator, "jwtValidator");
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        Instant start = Instant.now();
        try {
            SignedJWT signed = SignedJWT.parse(token);
            verifyAlgorithm(signed.getHeader());
            verifySignature(signed);

            Map<String, Object> headers = new HashMap<>(signed.getHeader().toJSONObject());
            Map<String, Object> claims = new HashMap<>(signed.getJWTClaimsSet().getClaims());

            Jwt jwt = Jwt.withTokenValue(token)
                    .headers(h -> h.putAll(headers))
                    .claims(c -> c.putAll(claims))
                    .issuedAt(instantClaim(claims, JwtClaimNames.IAT))
                    .expiresAt(instantClaim(claims, JwtClaimNames.EXP))
                    .build();

            OAuth2TokenValidatorResult baseValidation = jwtValidator.validate(jwt);
            if (baseValidation.hasErrors()) {
                throw new JwtException("Token validation failed: " + baseValidation.getErrors());
            }

            OAuth2TokenValidatorResult audValidation = validateAudience(jwt);
            if (audValidation.hasErrors()) {
                throw new JwtException("Token validation failed: " + audValidation.getErrors());
            }

            stats.onJwtDecode(Duration.between(start, Instant.now()));
            return jwt;
        } catch (ParseException e) {
            throw new JwtException("JWT parsing failed", e);
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("JWT decoding failed", e);
        }
    }

    private void verifyAlgorithm(JWSHeader header) {
        if (!JWSAlgorithm.RS256.equals(header.getAlgorithm())) {
            throw new JwtException("Unsupported alg: " + header.getAlgorithm());
        }
    }

    private void verifySignature(SignedJWT signed) throws ParseException {
        String kid = signed.getHeader().getKeyID();
        List<KeyRing.KeyEntry> candidates;
        if (kid == null) {
            candidates = keyRing.all();
        } else {
            KeyRing.KeyEntry entry = keyRing.findByKid(kid);
            candidates = entry == null ? List.of() : List.of(entry);
        }

        boolean verified = false;
        for (KeyRing.KeyEntry entry : candidates) {
            if (entry == null) {
                continue;
            }
            RSAPublicKey publicKey = entry.publicKey();
            try {
                verified = signed.verify(new RSASSAVerifier(publicKey));
            } catch (JOSEException e) {
                verified = false;
            }
            if (verified) {
                return;
            }
        }

        stats.onJwtSignatureFailure();
        throw new JwtException("JWT signature verification failed");
    }

    private OAuth2TokenValidatorResult validateAudience(Jwt jwt) {
        String requiredAudience = this.audience;
        if (requiredAudience == null || requiredAudience.isBlank()) {
            return OAuth2TokenValidatorResult.success();
        }
        List<String> audiences = jwt.getAudience();
        if (audiences != null && audiences.contains(requiredAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
    }

    private static Instant instantClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        return null;
    }
}
