package com.example.mcp.capabilities.tools.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;

/**
 * Standard response object for MCP tool executions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResponse {
    
    private final boolean success;
    private final String errorCode;
    private final String errorMessage;
    private final Object data;
    private final ZonedDateTime timestamp;
    
    private ToolResponse(boolean success, String errorCode, 
                         String errorMessage, Object data) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.data = data;
        this.timestamp = ZonedDateTime.now();
    }
    
    public static ToolResponse success(Object data) {
        return new ToolResponse(true, null, null, data);
    }
    
    public static ToolResponse error(ErrorCode code, String message) {
        return new ToolResponse(false, code.name(), message, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public Object getData() {
        return data;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public enum ErrorCode {
        VALIDATION_ERROR,
        NOT_FOUND,
        OPERATION_FAILED,
        SERVICE_UNAVAILABLE,
        TIMEOUT,
        EXECUTION_FAILED,
        INTERNAL_ERROR,
        UNAUTHORIZED,
        FORBIDDEN
    }
}
