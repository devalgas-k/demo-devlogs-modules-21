package com.example.pagination;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/** Opaque to clients, authenticated, NOT encrypted. Sort and schema are fixed by v1. */
public final class CursorCodec {
    private final byte[] key;
    private final Clock clock;
    public CursorCodec(byte[] key, Clock clock) {
        if (key.length < 32) throw new IllegalArgumentException("HMAC key must contain at least 32 bytes");
        this.key = key.clone(); this.clock = clock;
    }
    public String encode(long tenant, int size, FeedRepository.Boundary boundary) {
        String payload = "v1|" + tenant + "|" + size + "|" + clock.instant().plus(Duration.ofMinutes(15)).getEpochSecond()
                + "|" + boundary.createdAt() + "|" + boundary.id();
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        return b64(bytes) + "." + b64(sign(bytes));
    }
    public FeedRepository.Boundary decode(String token, long tenant, int size) {
        try {
            if (token.length() > 1024) throw new IllegalArgumentException();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 2) throw new IllegalArgumentException();
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            if (!MessageDigest.isEqual(sign(payload), Base64.getUrlDecoder().decode(parts[1])))
                throw new IllegalArgumentException();
            String[] fields = new String(payload, StandardCharsets.UTF_8).split("\\|", -1);
            if (fields.length != 6 || !fields[0].equals("v1") || Long.parseLong(fields[1]) != tenant
                    || Integer.parseInt(fields[2]) != size || Long.parseLong(fields[3]) <= clock.instant().getEpochSecond())
                throw new IllegalArgumentException();
            return new FeedRepository.Boundary(LocalDateTime.parse(fields[4]), Long.parseLong(fields[5]));
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("invalid, expired, or mismatched cursor", invalid);
        }
    }
    private byte[] sign(byte[] bytes) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(bytes);
        } catch (java.security.GeneralSecurityException impossible) {
            throw new IllegalStateException("HMAC unavailable", impossible);
        }
    }
    private static String b64(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
}
