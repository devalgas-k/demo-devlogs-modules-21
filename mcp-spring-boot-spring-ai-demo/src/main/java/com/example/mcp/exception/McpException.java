package com.example.mcp.exception;

import com.example.mcp.dto.McpError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base exception for MCP protocol errors.
 * 
 * All MCP-specific exceptions should extend this class to ensure
 * consistent error handling and logging across the server.
 */
public class McpException extends RuntimeException {

    private static final Logger log = LoggerFactory.getLogger(McpException.class);

    private final McpError error;
    private final int errorCode;
    private final String errorDetails;

    public McpException(McpError error) {
        super(error.getMessage());
        this.error = error;
        this.errorCode = error.getCode();
        this.errorDetails = null;
        log.debug("McpException created: {}", this);
    }

    public McpException(McpError error, String message) {
        super(message);
        this.error = error;
        this.errorCode = error.getCode();
        this.errorDetails = null;
        log.debug("McpException created: {} with message: {}", error, message);
    }

    public McpException(McpError error, String message, String details) {
        super(message);
        this.error = error;
        this.errorCode = error.getCode();
        this.errorDetails = details;
        log.debug("McpException created: {} with message: {} and details: {}", error, message, details);
    }

    public McpException(McpError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.errorCode = error.getCode();
        this.errorDetails = cause != null ? cause.getMessage() : null;
        log.debug("McpException created: {} with message: {} and cause: {}", error, message, cause.getMessage());
    }

    public McpException(McpError error, Throwable cause) {
        super(error.getMessage(), cause);
        this.error = error;
        this.errorCode = error.getCode();
        this.errorDetails = cause != null ? cause.getMessage() : null;
        log.debug("McpException created: {} with cause: {}", error, cause.getMessage());
    }

    /**
     * Returns the MCP error enum value.
     */
    public McpError getError() {
        return error;
    }

    /**
     * Returns the numeric error code.
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * Returns additional error details.
     */
    public String getErrorDetails() {
        return errorDetails;
    }

    /**
     * Returns the error code from the MCP error enum.
     */
    public int getCode() {
        return error.getCode();
    }

    @Override
    public String toString() {
        if (errorDetails != null) {
            return "McpException{error=%s, code=%d, message='%s', details='%s'}".formatted(
                error, errorCode, getMessage(), errorDetails);
        }
        return "McpException{error=%s, code=%d, message='%s'}".formatted(
            error, errorCode, getMessage());
    }
}
