package com.example.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-RPC 2.0 response DTO for MCP protocol.
 * 
 * A response contains either:
 * - result: successful execution
 * - error: failed execution
 * 
 * Cannot have both result and error.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface McpResponse 
    permits McpResponse.Success, McpResponse.Error {

    @JsonProperty("jsonrpc")
    String jsonrpc();

    @JsonProperty("id")
    Object id();

    /**
     * Successful response with result data.
     */
    record Success(
        @JsonProperty("jsonrpc") String jsonrpc,
        @JsonProperty("id") Object id,
        @JsonProperty("result") Object result
    ) implements McpResponse {
        
        public static Success of(Object id, Object result) {
            return new Success("2.0", id, result);
        }
    }

    /**
     * Error response with error details.
     */
    record Error(
        @JsonProperty("jsonrpc") String jsonrpc,
        @JsonProperty("id") Object id,
        @JsonProperty("error") ErrorDetail error
    ) implements McpResponse {
        
        public static Error of(Object id, McpError code, String message) {
            return new Error("2.0", id, new ErrorDetail(code.getCode(), code.getMessage(), null));
        }

        public static Error of(Object id, McpError code, String message, Object data) {
            return new Error("2.0", id, new ErrorDetail(code.getCode(), message, data));
        }

        public static Error of(Object id, int code, String message) {
            return new Error("2.0", id, new ErrorDetail(code, message, null));
        }
    }

    /**
     * Error detail structure for error responses.
     */
    record ErrorDetail(
        @JsonProperty("code") int code,
        @JsonProperty("message") String message,
        @JsonProperty("data") Object data
    ) {}

    /**
     * Creates a success response.
     */
    static McpResponse success(Object id, Object result) {
        return Success.of(id, result);
    }

    /**
     * Creates an error response from an McpError code.
     */
    static McpResponse error(Object id, McpError mcpError) {
        return Error.of(id, mcpError, mcpError.getMessage());
    }

    /**
     * Creates an error response from an McpError code with custom message.
     */
    static McpResponse error(Object id, McpError mcpError, String message) {
        return Error.of(id, mcpError, message);
    }

    /**
     * Creates an error response with data.
     */
    static McpResponse error(Object id, McpError mcpError, String message, Object data) {
        return Error.of(id, mcpError, message, data);
    }

    /**
     * Creates a parse error response.
     */
    static McpResponse parseError(String message) {
        return Error.of(null, McpError.PARSE_ERROR, message);
    }

    /**
     * Creates an invalid request error response.
     */
    static McpResponse invalidRequest(Object id, String message) {
        return Error.of(id, McpError.INVALID_REQUEST, message);
    }
}
