package com.example.mcp.server;

import com.example.mcp.dto.McpRequest;
import com.example.mcp.dto.McpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * REST + SSE endpoint for MCP server.
 * Handles HTTP requests and SSE streaming responses.
 */
@RestController
@RequestMapping("/mcp")
public class McpServerEndpoint {

    private static final Logger log = LoggerFactory.getLogger(McpServerEndpoint.class);

    private final McpMessageHandler messageHandler;
    private final SseEmitterProcessor sseProcessor;
    private final SessionManager sessionManager;

    public McpServerEndpoint(
            McpMessageHandler messageHandler,
            SseEmitterProcessor sseProcessor,
            SessionManager sessionManager) {
        this.messageHandler = messageHandler;
        this.sseProcessor = sseProcessor;
        this.sessionManager = sessionManager;
    }

    /**
     * HTTP POST endpoint for JSON-RPC requests.
     * Used for synchronous request/response pattern.
     */
    @PostMapping("/rpc")
    public McpResponse handleRpc(@RequestBody McpRequest request) {
        log.debug("Received JSON-RPC request: method={}, id={}", 
                request.getMethod(), request.getId());
        
        return messageHandler.handle(request);
    }

    /**
     * SSE streaming endpoint for server-sent events.
     * Used for async responses and server-initiated notifications.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String sessionId) {
        String effectiveSessionId = sessionId != null ? sessionId : sessionManager.createSession();
        
        log.info("Opening SSE stream for session={}", effectiveSessionId);
        
        SseEmitter emitter = sseProcessor.createEmitter(effectiveSessionId);
        
        emitter.onCompletion(() -> {
            log.info("SSE stream completed for session={}", effectiveSessionId);
            sessionManager.closeSession(effectiveSessionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE stream timed out for session={}", effectiveSessionId);
            sessionManager.closeSession(effectiveSessionId);
        });
        
        emitter.onError(e -> {
            log.error("SSE stream error for session={}: {}", effectiveSessionId, e.getMessage());
            sessionManager.closeSession(effectiveSessionId);
        });
        
        sessionManager.registerSseEmitter(effectiveSessionId, emitter);
        
        return emitter;
    }

    /**
     * Async POST endpoint for long-running operations.
     * Returns immediately with a session ID for tracking.
     */
    @PostMapping("/rpc/async")
    public CompletableFuture<McpResponse> handleAsyncRpc(@RequestBody McpRequest request) {
        log.debug("Received async JSON-RPC request: method={}, id={}", 
                request.getMethod(), request.getId());
        
        return messageHandler.handleAsync(request);
    }

    /**
     * Send notification to a specific session via SSE.
     * Used for server-initiated messages.
     */
    public void sendNotification(String sessionId, McpResponse notification) {
        SseEmitter emitter = sessionManager.getSseEmitter(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(notification));
                log.debug("Notification sent to session={}", sessionId);
            } catch (IOException e) {
                log.warn("Failed to send notification to session={}: {}", 
                        sessionId, e.getMessage());
                sessionManager.closeSession(sessionId);
            }
        } else {
            log.warn("No active SSE emitter for session={}", sessionId);
        }
    }

    /**
     * Broadcast message to all connected sessions.
     */
    public void broadcast(McpResponse message) {
        sessionManager.getAllSessionIds().forEach(sessionId -> 
            sendNotification(sessionId, message));
    }

    /**
     * Health check endpoint for the MCP server.
     */
    @GetMapping("/health")
    public HealthStatus health() {
        return new HealthStatus(
                "UP",
                sessionManager.getActiveSessionCount(),
                sseProcessor.getConnectionCount()
        );
    }

    public record HealthStatus(String status, int activeSessions, int sseConnections) {}
}
