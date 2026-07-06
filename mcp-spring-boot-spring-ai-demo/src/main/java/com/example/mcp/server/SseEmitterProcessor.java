package com.example.mcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Manages SSE (Server-Sent Events) connections with heartbeat,
 * connection limits, and graceful cleanup.
 */
@Component
public class SseEmitterProcessor {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterProcessor.class);
    
    private static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_CONNECTIONS = 1000;

    private final Duration heartbeatInterval;
    private final Duration connectionTimeout;
    private final int maxConnections;
    
    private final ConcurrentMap<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> connectionTimestamps = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService heartbeatScheduler;
    private final Semaphore connectionSemaphore;

    public SseEmitterProcessor() {
        this(DEFAULT_HEARTBEAT_INTERVAL, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_MAX_CONNECTIONS);
    }

    public SseEmitterProcessor(Duration heartbeatInterval, Duration connectionTimeout, int maxConnections) {
        this.heartbeatInterval = heartbeatInterval;
        this.connectionTimeout = connectionTimeout;
        this.maxConnections = maxConnections;
        this.connectionSemaphore = new Semaphore(maxConnections);
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat-scheduler");
            t.setDaemon(true);
            return t;
        });
        
        startHeartbeatTask();
        
        log.info("SseEmitterProcessor initialized: heartbeat={}, timeout={}, maxConnections={}",
                heartbeatInterval, connectionTimeout, maxConnections);
    }

    /**
     * Create a new SSE emitter with heartbeat support.
     */
    public SseEmitter createEmitter(String sessionId) {
        if (!connectionSemaphore.tryAcquire()) {
            log.warn("Connection limit reached, rejecting new connection: {}", maxConnections);
            throw new ConnectionLimitExceededException(
                    "Maximum connections reached: " + maxConnections);
        }

        long timeout = connectionTimeout.toMillis();
        SseEmitter emitter = new SseEmitter(timeout);
        
        activeEmitters.put(sessionId, emitter);
        connectionTimestamps.put(sessionId, System.currentTimeMillis());
        
        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed: sessionId={}", sessionId);
            cleanupConnection(sessionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out: sessionId={}", sessionId);
            cleanupConnection(sessionId);
        });
        
        emitter.onError(e -> {
            log.error("SSE emitter error: sessionId={}, error={}", sessionId, e.getMessage());
            cleanupConnection(sessionId);
        });
        
        log.info("SSE emitter created: sessionId={}, activeConnections={}", 
                sessionId, activeEmitters.size());
        
        return emitter;
    }

    /**
     * Send data to a specific session.
     */
    public void send(String sessionId, Object data) {
        SseEmitter emitter = activeEmitters.get(sessionId);
        if (emitter == null) {
            log.warn("No active emitter for sessionId={}", sessionId);
            return;
        }
        
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(data));
            
            connectionTimestamps.put(sessionId, System.currentTimeMillis());
            log.debug("SSE data sent: sessionId={}", sessionId);
        } catch (IOException e) {
            log.warn("Failed to send SSE data, cleaning up: sessionId={}, error={}", 
                    sessionId, e.getMessage());
            cleanupConnection(sessionId);
        }
    }

    /**
     * Broadcast data to all connected sessions.
     */
    public void broadcast(Object data) {
        activeEmitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(data));
            } catch (IOException e) {
                log.warn("Broadcast failed for sessionId={}: {}", sessionId, e.getMessage());
                cleanupConnection(sessionId);
            }
        });
    }

    /**
     * Get current connection count.
     */
    public int getConnectionCount() {
        return activeEmitters.size();
    }

    /**
     * Check if a session has an active connection.
     */
    public boolean isConnected(String sessionId) {
        return activeEmitters.containsKey(sessionId);
    }

    /**
     * Get remaining connection capacity.
     */
    public int getRemainingCapacity() {
        return connectionSemaphore.availablePermits();
    }

    private void cleanupConnection(String sessionId) {
        SseEmitter removed = activeEmitters.remove(sessionId);
        connectionTimestamps.remove(sessionId);
        connectionSemaphore.release();
        
        if (removed != null) {
            try {
                removed.complete();
            } catch (Exception e) {
                log.debug("Error completing removed emitter: sessionId={}", sessionId);
            }
            
            log.info("Connection cleaned up: sessionId={}, remainingConnections={}", 
                    sessionId, activeEmitters.size());
        }
    }

    private void startHeartbeatTask() {
        heartbeatScheduler.scheduleAtFixedRate(
                this::sendHeartbeats,
                heartbeatInterval.toMillis(),
                heartbeatInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
        
        log.debug("Heartbeat task started: interval={}", heartbeatInterval);
    }

    private void sendHeartbeats() {
        try {
            long now = System.currentTimeMillis();
            long timeoutMillis = connectionTimeout.toMillis();
            
            activeEmitters.forEach((sessionId, emitter) -> {
                Long lastActivity = connectionTimestamps.get(sessionId);
                if (lastActivity != null) {
                    long elapsed = now - lastActivity;
                    
                    if (elapsed > timeoutMillis) {
                        log.warn("Connection timeout, cleaning up: sessionId={}, elapsed={}ms", 
                                sessionId, elapsed);
                        cleanupConnection(sessionId);
                    } else {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("heartbeat")
                                    .comment("keepalive"));
                            log.trace("Heartbeat sent: sessionId={}", sessionId);
                        } catch (IOException e) {
                            log.warn("Heartbeat failed, cleaning up: sessionId={}", sessionId);
                            cleanupConnection(sessionId);
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.error("Error during heartbeat: {}", e.getMessage(), e);
        }
    }

    /**
     * Gracefully shutdown all connections.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SseEmitterProcessor, closing {} connections", activeEmitters.size());
        
        heartbeatScheduler.shutdown();
        
        activeEmitters.forEach((sessionId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("shutdown")
                        .data("Server shutting down"));
                emitter.complete();
            } catch (IOException e) {
                log.debug("Error sending shutdown event: sessionId={}", sessionId);
            }
        });
        
        activeEmitters.clear();
        
        try {
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("SseEmitterProcessor shutdown complete");
    }

    public static class ConnectionLimitExceededException extends RuntimeException {
        public ConnectionLimitExceededException(String message) {
            super(message);
        }
    }
}
