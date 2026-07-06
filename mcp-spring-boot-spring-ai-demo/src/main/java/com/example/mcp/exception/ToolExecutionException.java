package com.example.mcp.exception;

import com.example.mcp.dto.McpError;

/**
 * Exception thrown when tool execution fails.
 * 
 * This exception indicates that a tool was found and invoked,
 * but failed during execution due to:
 * - Business logic errors
 * - External service failures
 * - Validation failures within the tool
 * - Timeout during execution
 */
public class ToolExecutionException extends McpException {

    private final String toolName;
    private final Object[] inputParameters;

    public ToolExecutionException(String toolName, String message) {
        super(McpError.TOOL_EXECUTION_ERROR, message);
        this.toolName = toolName;
        this.inputParameters = null;
    }

    public ToolExecutionException(String toolName, String message, String details) {
        super(McpError.TOOL_EXECUTION_ERROR, message, details);
        this.toolName = toolName;
        this.inputParameters = null;
    }

    public ToolExecutionException(String toolName, String message, Throwable cause) {
        super(McpError.TOOL_EXECUTION_ERROR, message, cause);
        this.toolName = toolName;
        this.inputParameters = null;
    }

    public ToolExecutionException(String toolName, Object[] inputParameters, String message, Throwable cause) {
        super(McpError.TOOL_EXECUTION_ERROR, message, cause);
        this.toolName = toolName;
        this.inputParameters = inputParameters;
    }

    /**
     * Creates an exception for tool not found.
     */
    public static ToolExecutionException notFound(String toolName) {
        return new ToolExecutionException(
            toolName, 
            McpError.TOOL_NOT_FOUND.getMessage() + ": " + toolName
        );
    }

    /**
     * Creates an exception for tool timeout.
     */
    public static ToolExecutionException timeout(String toolName, long timeoutSeconds) {
        return new ToolExecutionException(
            toolName,
            McpError.TOOL_TIMEOUT.getMessage() + " after " + timeoutSeconds + "s",
            "Execution exceeded maximum allowed time"
        );
    }

    /**
     * Creates an exception for validation failure.
     */
    public static ToolExecutionException validationFailed(String toolName, String validationMessage) {
        return new ToolExecutionException(
            toolName,
            McpError.VALIDATION_ERROR.getMessage() + ": " + validationMessage,
            validationMessage
        );
    }

    /**
     * Creates an exception for service unavailable.
     */
    public static ToolExecutionException serviceUnavailable(String toolName, String serviceName) {
        return new ToolExecutionException(
            toolName,
            McpError.SERVICE_UNAVAILABLE.getMessage(),
            "Required service '" + serviceName + "' is unavailable"
        );
    }

    /**
     * Returns the tool name that failed.
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Returns the input parameters (may be null for security reasons).
     */
    public Object[] getInputParameters() {
        return inputParameters;
    }

    @Override
    public String toString() {
        return "ToolExecutionException{toolName='%s', error=%s, message='%s'}"
            .formatted(toolName, getError(), getMessage());
    }
}
