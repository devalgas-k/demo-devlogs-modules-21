package com.example.mcp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Objects;

/**
 * Request DTO for tool execution via MCP protocol.
 * 
 * Contains the tool name and parameters to be executed.
 */
public record ToolRequest(
    @JsonProperty("name")
    @NotBlank(message = "Tool name must not be blank")
    String name,

    @JsonProperty("parameters")
    Map<String, Object> parameters,

    @JsonProperty("sessionId")
    String sessionId,

    @JsonProperty("requestId")
    String requestId
) {
    /**
     * Creates a new ToolRequest with the given tool name and parameters.
     */
    public static ToolRequest of(String name, Map<String, Object> parameters) {
        return new ToolRequest(name, parameters, null, null);
    }

    /**
     * Creates a new ToolRequest with the given tool name, parameters, and session ID.
     */
    public static ToolRequest of(String name, Map<String, Object> parameters, String sessionId) {
        return new ToolRequest(name, parameters, sessionId, null);
    }

    /**
     * Returns the parameter value cast to the expected type.
     * @param key Parameter key
     * @param type Expected type class
     * @return The parameter value or null if not found or type mismatch
     */
    public <T> T getParameter(String key, Class<T> type) {
        if (parameters == null || !parameters.containsKey(key)) {
            return null;
        }
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    /**
     * Returns the parameter value as a String.
     * @param key Parameter key
     * @return The parameter value or null if not found
     */
    public String getString(String key) {
        return getParameter(key, String.class);
    }

    /**
     * Returns the parameter value as an Integer.
     * @param key Parameter key
     * @return The parameter value or null if not found
     */
    public Integer getInt(String key) {
        return getParameter(key, Integer.class);
    }

    /**
     * Returns the parameter value as a Boolean.
     * @param key Parameter key
     * @return The parameter value or null if not found
     */
    public Boolean getBoolean(String key) {
        return getParameter(key, Boolean.class);
    }

    /**
     * Builder for ToolRequest.
     */
    public static final class Builder {
        private String name;
        private Map<String, Object> parameters;
        private String sessionId;
        private String requestId;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public ToolRequest build() {
            return new ToolRequest(name, parameters, sessionId, requestId);
        }
    }

    public Builder toBuilder() {
        return new Builder()
            .name(this.name)
            .parameters(this.parameters)
            .sessionId(this.sessionId)
            .requestId(this.requestId);
    }
}
