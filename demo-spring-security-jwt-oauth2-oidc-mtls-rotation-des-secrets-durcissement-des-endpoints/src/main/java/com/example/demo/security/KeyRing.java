package com.example.demo.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class KeyRing {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Deque<KeyEntry> entries = new ArrayDeque<>();

    public KeyRing() {
        rotate();
    }

    public KeyEntry current() {
        lock.readLock().lock();
        try {
            return entries.getFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<KeyEntry> all() {
        lock.readLock().lock();
        try {
            return List.copyOf(entries);
        } finally {
            lock.readLock().unlock();
        }
    }

    public KeyEntry rotate() {
        lock.writeLock().lock();
        try {
            KeyPair keyPair = generateRsaKeyPair();
            String kid = UUID.randomUUID().toString();
            KeyEntry entry = new KeyEntry(kid, (RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate(), Instant.now());
            entries.addFirst(entry);
            while (entries.size() > 3) {
                entries.removeLast();
            }
            return entry;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public KeyEntry findByKid(String kid) {
        Objects.requireNonNull(kid, "kid");
        lock.readLock().lock();
        try {
            return entries.stream().filter(e -> e.kid().equals(kid)).findFirst().orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA not available", e);
        }
    }

    public record KeyEntry(String kid, RSAPublicKey publicKey, RSAPrivateKey privateKey, Instant createdAt) {
    }
}
