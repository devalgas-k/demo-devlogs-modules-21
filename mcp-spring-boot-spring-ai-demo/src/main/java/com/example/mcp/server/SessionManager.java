package com.example.mcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages stateful sessions for MCP clients.
 * Handles session creation, timeout, heartbeat, and cleanup.
 */
@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SseEmitter> sessionEmitters = new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler;
    private final Duration sessionTimeout;
    private final Duration heartbeatInterval;
    private final int maxSessions;

    public SessionManager() {
        this(Duration.ofMinutes(10), Duration.ofSeconds(30), 1000);
    }

    public SessionManager(Duration sessionTimeout, Duration heartbeatInterval, int maxSessions) {
        this.sessionTimeout = sessionTimeout;
        this.heartbeatInterval = heartbeatInterval;
        this.maxSessions = maxSessions;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "session-manager-scheduler");
            t.setDaemon(true);
            return t;
        });
        
        startCleanupTask();
        
        log.info("SessionManager initialized: timeout={}, heartbeat={}, maxSessions={}",
                sessionTimeout, heartbeatInterval, maxSessions);
    }

    /**
     * Create a new session.
     */
    public String createSession() {
        if (sessions.size() >= maxSessions) {
            log.warn("Cannot create session: max sessions limit reached ({})", maxSessions);
            throw new SessionLimitExceededException("Maximum session limit exceeded: " + maxSessions);
        }

        String sessionId = generateSessionId();
        Session session = new Session(sessionId, Instant.now());
        sessions.put(sessionId, session);
        
        log.info("Session created: id={}, totalActive={}", sessionId, sessions.size());
        
        return sessionId;
    }

    /**
     * Close a session and clean up resources.
     */
    public void closeSession(String sessionId) {
        Session session = sessions.remove(sessionId);
        SseEmitter emitter = sessionEmitters.remove(sessionId);
        
        if (session != null) {
            log.info("Session closed: id={}, lifetime={}ms", 
                    sessionId, 
                    Duration.between(session.createdAt(), Instant.now()).toMillis());
        }
        
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Error completing emitter for session={}: {}", sessionId, e.getMessage());
            }
        }
    }

    /**
     * Register an SSE emitter for a session.
     */
    public void registerSseEmitter(String sessionId, SseEmitter emitter) {
        sessionEmitters.put(sessionId, emitter);
        
        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for session={}", sessionId);
            closeSession(sessionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out for session={}", sessionId);
            closeSession(sessionId);
        });
        
        emitter.onError(e -> {
            log.error("SSE emitter error for session={}: {}", sessionId, e.getMessage());
            closeSession(sessionId);
        });
        
        log.debug("SSE emitter registered for session={}", sessionId);
    }

    /**
     * Get the SSE emitter for a session.
     */
    public SseEmitter getSseEmitter(String sessionId) {
        return sessionEmitters.get(sessionId);
    }

    /**
     * Get a session by ID.
     */
    public Session getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Update session last activity timestamp.
     */
    public void touchSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.setLastActivity(Instant.now());
            log.trace("Session touched: id={}", sessionId);
        }
    }

    /**
     * Send heartbeat to keep session alive.
     */
    public void heartbeat(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.setLastHeartbeat(Instant.now());
            touchSession(sessionId);
            log.trace("Heartbeat received: id={}", sessionId);
        }
    }

    /**
     * Get count of active sessions.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Get all active session IDs.
     */
    public java.util.Set<String> getAllSessionIds() {
        return java.util.Collections.unmodifiableSet(sessions.keySet());
    }

    /**
     * Get orphaned sessions (no heartbeat within threshold).
     */
    public java.util.List<String> getOrphanedSessions(Duration threshold) {
        Instant cutoff = Instant.now().minus(threshold);
        return sessions.entrySet().stream()
                .filter(e -> e.getValue().lastHeartbeat().isBefore(cutoff))
                .map(Map.Entry::getKey)
                .toList();
    }

    private void startCleanupTask() {
        scheduler.scheduleAtFixedRate(
                this::cleanupOrphanedSessions,
                heartbeatInterval.toMillis(),
                heartbeatInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );
        
        log.debug("Cleanup task scheduled: interval={}", heartbeatInterval);
    }

    private void cleanupOrphanedSessions() {
        try {
            Instant cutoff = Instant.now().minus(sessionTimeout);
            int orphanedCount = 0;
            
            for (Map.Entry<String, Session> entry : sessions.entrySet()) {
                if (entry.getValue().lastHeartbeat().isBefore(cutoff)) {
                    log.warn("Cleaning up orphaned session: id={}, lastHeartbeat={}", 
                            entry.getKey(), entry.getValue().lastHeartbeat());
                    closeSession(entry.getKey());
                    orphanedCount++;
                }
            }
            
            if (orphanedCount > 0) {
                log.info("Cleaned up {} orphaned sessions", orphanedCount);
            }
        } catch (Exception e) {
            log.error("Error during orphaned session cleanup: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SessionManager, closing {} active sessions", sessions.size());
        
        scheduler.shutdown();
        
        sessions.keySet().forEach(this::closeSession);
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("SessionManager shutdown complete");
    }

    private String generateSessionId() {
        return "sess-" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Session state holder.
     */
    public static class Session {
        private final String id;
        private final Instant createdAt;
        private Instant lastActivity;
        private Instant lastHeartbeat;

        public Session(String id, Instant createdAt) {
            this.id = id;
            this.createdAt = createdAt;
            this.lastActivity = createdAt;
            this.lastHeartbeat = createdAt;
        }

        public String id() { return id; }
        public Instant createdAt() { return createdAt; }
        public Instant lastActivity() { return lastActivity; }
        public Instant lastHeartbeat() { return lastHeartbeat; }

        public void setLastActivity(Instant lastActivity) {
            this.lastActivity = lastActivity;
        }

        public void setLastHeartbeat(Instant lastHeartbeat) {
            this.lastHeartbeat = lastHeartbeat;
        }
    }

    public static class SessionLimitExceededException extends RuntimeException {
        public SessionLimitExceededException(String message) {
            super(message);
        }
    }
}
