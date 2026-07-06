package com.example.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC 2.0 request DTO for MCP protocol.
 * 
 * @param jsonrpc Must be exactly "2.0"
 * @param id Request identifier (can be string or number)
 * @param method The method to invoke
 * @param params Optional parameters for the method
 */
public record McpRequest(
    @JsonProperty("jsonrpc")
    @NotBlank(message = "jsonrpc field must be present and non-blank")
    String jsonrpc,

    @JsonProperty("id")
    @NotNull(message = "id field must be present")
    Object id,

    @JsonProperty("method")
    @NotBlank(message = "method field must be present and non-blank")
    String method,

    @JsonProperty("params")
    Map<String, Object> params
) {
    /**
     * Validates that this is a valid JSON-RPC 2.0 request.
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return "2.0".equals(jsonrpc) 
            && id != null 
            && method != null 
            && !method.isBlank();
    }

    /**
     * Creates a new builder with current values.
     */
    public Builder toBuilder() {
        return new Builder(jsonrpc, id, method, params);
    }

    /**
     * Factory method to create a new request with default jsonrpc version.
     */
    public static McpRequest of(String method, Object id, Map<String, Object> params) {
        return new McpRequest("2.0", id, method, params);
    }

    public static McpRequest of(String method, Object id) {
        return new McpRequest("2.0", id, method, null);
    }

    public static final class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private Map<String, Object> params;

        public Builder() {}

        private Builder(String jsonrpc, Object id, String method, Map<String, Object> params) {
            this.jsonrpc = jsonrpc;
            this.id = id;
            this.method = method;
            this.params = params;
        }

        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public McpRequest build() {
            return new McpRequest(jsonrpc, id, method, params);
        }
    }
}
