package com.example.mcp.capabilities.tools.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a method as an MCP tool.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpTool {
    
    /**
     * The unique name of the tool.
     */
    String name();
    
    /**
     * Human-readable description of what the tool does.
     */
    String description() default "";
    
    /**
     * Optional tags for categorization.
     */
    String[] tags() default {};
}
