package com.example.mcp.capabilities.tools.executor;

import com.example.mcp.capabilities.tools.response.ToolResponse;

/**
 * Interface for MCP tool executors.
 */
public interface ToolExecutor {
    
    /**
     * Execute the tool with the given parameters.
     * 
     * @param params Tool parameters as name-value pairs
     * @return ToolResponse containing the result or error
     */
    ToolResponse execute(Object... params);
}
