package com.example.mcp.exception;

import com.example.mcp.dto.McpError;

/**
 * Exception thrown when a session is not found or has expired.
 * 
 * This exception indicates that:
 * - The session ID provided does not exist
 * - The session has expired due to timeout
 * - The session was explicitly invalidated
 */
public class SessionNotFoundException extends McpException {

    private final String sessionId;
    private final String reason;

    public SessionNotFoundException(String sessionId) {
        super(McpError.SESSION_NOT_FOUND, "Session not found: " + sessionId);
        this.sessionId = sessionId;
        this.reason = "Session does not exist";
    }

    public SessionNotFoundException(String sessionId, String reason) {
        super(McpError.SESSION_NOT_FOUND, reason != null ? reason : "Session not found: " + sessionId);
        this.sessionId = sessionId;
        this.reason = reason != null ? reason : "Session does not exist";
    }

    /**
     * Creates an exception for an expired session.
     */
    public static SessionNotFoundException expired(String sessionId) {
        return new SessionNotFoundException(
            sessionId,
            McpError.SESSION_EXPIRED.getMessage() + ": " + sessionId
        );
    }

    /**
     * Creates an exception when session limit is exceeded.
     */
    public static SessionNotFoundException limitExceeded(int maxSessions) {
        SessionNotFoundException ex = new SessionNotFoundException(
            null,
            McpError.SESSION_LIMIT_EXCEEDED.getMessage() + " (max: " + maxSessions + ")"
        );
        return ex;
    }

    /**
     * Returns the session ID that was not found.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the reason why the session was not found.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Checks if this exception has a valid session ID.
     */
    public boolean hasSessionId() {
        return sessionId != null && !sessionId.isBlank();
    }

    @Override
    public String toString() {
        if (sessionId != null) {
            return "SessionNotFoundException{sessionId='%s', reason='%s', error=%s}"
                .formatted(sessionId, reason, getError());
        }
        return "SessionNotFoundException{reason='%s', error=%s}"
            .formatted(reason, getError());
    }
}
