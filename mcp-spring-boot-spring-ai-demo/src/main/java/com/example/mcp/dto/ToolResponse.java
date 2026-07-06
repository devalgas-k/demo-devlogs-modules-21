package com.example.mcp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for tool execution via MCP protocol.
 * 
 * Contains the execution result or error information.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface ToolResponse 
    permits ToolResponse.Success, ToolResponse.Error {

    @JsonProperty("requestId")
    String requestId();

    @JsonProperty("timestamp")
    Instant timestamp();

    /**
     * Successful tool execution result.
     */
    record Success(
        @JsonProperty("requestId") String requestId,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("result") Object result,
        @JsonProperty("metadata") Map<String, Object> metadata
    ) implements ToolResponse {
        
        public static Success of(Object result) {
            return new Success(UUID.randomUUID().toString(), Instant.now(), result, null);
        }

        public static Success of(Object result, Map<String, Object> metadata) {
            return new Success(UUID.randomUUID().toString(), Instant.now(), result, metadata);
        }

        public static Success of(String requestId, Object result) {
            return new Success(requestId, Instant.now(), result, null);
        }

        public static Success of(String requestId, Object result, Map<String, Object> metadata) {
            return new Success(requestId, Instant.now(), result, metadata);
        }
    }

    /**
     * Error during tool execution.
     */
    record Error(
        @JsonProperty("requestId") String requestId,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("errorCode") int errorCode,
        @JsonProperty("errorMessage") String errorMessage,
        @JsonProperty("errorDetails") String errorDetails
    ) implements ToolResponse {

        public static Error of(int errorCode, String errorMessage) {
            return new Error(UUID.randomUUID().toString(), Instant.now(), errorCode, errorMessage, null);
        }

        public static Error of(int errorCode, String errorMessage, String errorDetails) {
            return new Error(UUID.randomUUID().toString(), Instant.now(), errorCode, errorMessage, errorDetails);
        }

        public static Error of(String requestId, int errorCode, String errorMessage) {
            return new Error(requestId, Instant.now(), errorCode, errorMessage, null);
        }

        public static Error of(String requestId, McpError mcpError) {
            return new Error(requestId, Instant.now(), mcpError.getCode(), mcpError.getMessage(), null);
        }

        public static Error of(String requestId, McpError mcpError, String details) {
            return new Error(requestId, Instant.now(), mcpError.getCode(), mcpError.getMessage(), details);
        }
    }

    /**
     * Creates a success response with the given result.
     */
    static ToolResponse success(Object result) {
        return Success.of(result);
    }

    /**
     * Creates a success response with result and metadata.
     */
    static ToolResponse success(Object result, Map<String, Object> metadata) {
        return Success.of(result, metadata);
    }

    /**
     * Creates an error response.
     */
    static ToolResponse error(int errorCode, String errorMessage) {
        return Error.of(errorCode, errorMessage);
    }

    /**
     * Creates an error response from McpError.
     */
    static ToolResponse error(McpError mcpError) {
        return Error.of(mcpError);
    }

    /**
     * Creates an error response from McpError with details.
     */
    static ToolResponse error(McpError mcpError, String details) {
        return Error.of(mcpError, details);
    }

    /**
     * Checks if this is a success response.
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Checks if this is an error response.
     */
    default boolean isError() {
        return this instanceof Error;
    }

    /**
     * Converts to Success if possible.
     */
    default Success asSuccess() {
        if (this instanceof Success s) {
            return s;
        }
        throw new IllegalStateException("Response is not a success");
    }

    /**
     * Converts to Error if possible.
     */
    default Error asError() {
        if (this instanceof Error e) {
            return e;
        }
        throw new IllegalStateException("Response is not an error");
    }
}
