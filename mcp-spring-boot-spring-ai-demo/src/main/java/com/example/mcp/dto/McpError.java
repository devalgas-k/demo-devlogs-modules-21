package com.example.mcp.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * MCP protocol error codes enumeration.
 * 
 * Follows JSON-RPC 2.0 error code conventions and extends with MCP-specific codes.
 * 
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 */
public enum McpError {
    
    // Standard JSON-RPC 2.0 errors (-32768 to -32000)
    PARSE_ERROR(-32700, "Parse error - Invalid JSON"),
    INVALID_REQUEST(-32600, "Invalid Request"),
    METHOD_NOT_FOUND(-32601, "Method not found"),
    INVALID_PARAMS(-32602, "Invalid params"),
    INTERNAL_ERROR(-32603, "Internal error"),

    // MCP-specific errors (-32000 to -32099)
    TOOL_NOT_FOUND(-32001, "Tool not found"),
    TOOL_EXECUTION_ERROR(-32002, "Tool execution failed"),
    TOOL_TIMEOUT(-32003, "Tool execution timed out"),
    TOOL_CANCELLED(-32004, "Tool execution was cancelled"),
    RESOURCE_NOT_FOUND(-32005, "Resource not found"),
    RESOURCE_NOT_READABLE(-32006, "Resource not readable"),
    PROMPT_NOT_FOUND(-32007, "Prompt not found"),
    SESSION_NOT_FOUND(-32008, "Session not found"),
    SESSION_EXPIRED(-32009, "Session expired"),
    SESSION_LIMIT_EXCEEDED(-32010, "Maximum session count exceeded"),
    SERVICE_UNAVAILABLE(-32011, "Service temporarily unavailable"),
    UNAUTHORIZED(-32012, "Unauthorized - authentication required"),
    FORBIDDEN(-32013, "Forbidden - insufficient permissions"),
    RATE_LIMIT_EXCEEDED(-32014, "Rate limit exceeded"),
    VALIDATION_ERROR(-32015, "Input validation failed"),
    TOOL_NOT_AVAILABLE(-32016, "Tool not available");

    private final int code;
    private final String message;

    McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * Returns the numeric error code.
     */
    @JsonValue
    public int getCode() {
        return code;
    }

    /**
     * Returns the error message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Finds an McpError by its code.
     * @param code The error code to find
     * @return The corresponding McpError or null if not found
     */
    public static McpError fromCode(int code) {
        for (McpError error : values()) {
            if (error.code == code) {
                return error;
            }
        }
        return null;
    }

    /**
     * Checks if this is a JSON-RPC standard error.
     */
    public boolean isJsonRpcStandard() {
        return code >= -32768 && code <= -32600;
    }

    /**
     * Checks if this is an MCP-specific error.
     */
    public boolean isMcpSpecific() {
        return code >= -32099 && code <= -32000;
    }

    @Override
    public String toString() {
        return "McpError{code=%d, message='%s'}".formatted(code, message);
    }
}
