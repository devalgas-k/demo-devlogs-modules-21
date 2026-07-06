package com.example.mcp.server;

import com.example.mcp.dto.McpRequest;
import com.example.mcp.dto.McpResponse;
import com.example.mcp.dto.McpError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Handles JSON-RPC 2.0 message routing.
 * Routes incoming requests to appropriate capabilities (tools, resources, prompts).
 */
@Component
public class McpMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(McpMessageHandler.class);

    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final ResourceRegistry resourceRegistry;
    private final PromptRegistry promptRegistry;

    public McpMessageHandler(
            ObjectMapper objectMapper,
            ToolRegistry toolRegistry,
            ResourceRegistry resourceRegistry,
            PromptRegistry promptRegistry) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.resourceRegistry = resourceRegistry;
        this.promptRegistry = promptRegistry;
    }

    /**
     * Handle a JSON-RPC request synchronously.
     */
    public McpResponse handle(McpRequest request) {
        if (request == null) {
            return createParseError(null);
        }

        if (!isValidRequest(request)) {
            return createInvalidRequestError(request);
        }

        String method = request.getMethod();
        log.debug("Routing JSON-RPC request: method={}, id={}", method, request.getId());

        try {
            return switch (method) {
                case "initialize" -> handleInitialize(request);
                case "tools/list" -> handleToolsList(request);
                case "tools/call" -> handleToolsCall(request);
                case "resources/list" -> handleResourcesList(request);
                case "resources/read" -> handleResourcesRead(request);
                case "prompts/list" -> handlePromptsList(request);
                case "prompts/get" -> handlePromptsGet(request);
                case "ping" -> handlePing(request);
                default -> createMethodNotFoundError(request, method);
            };
        } catch (Exception e) {
            log.error("Error handling request method={}: {}", method, e.getMessage(), e);
            return createInternalError(request, e.getMessage());
        }
    }

    /**
     * Handle a JSON-RPC request asynchronously.
     * Used for long-running operations.
     */
    public CompletableFuture<McpResponse> handleAsync(McpRequest request) {
        return CompletableFuture.supplyAsync(() -> handle(request));
    }

    /**
     * Handle notifications (requests without id - no response required).
     */
    public void handleNotification(McpRequest request) {
        if (request == null || request.getMethod() == null) {
            log.warn("Received invalid notification");
            return;
        }

        log.debug("Handling notification: method={}", request.getMethod());
        
        try {
            switch (request.getMethod()) {
                case "initialized" -> handleInitialized(request);
                case "shutdown" -> handleShutdown(request);
                default -> log.warn("Unknown notification method: {}", request.getMethod());
            }
        } catch (Exception e) {
            log.error("Error handling notification {}: {}", request.getMethod(), e.getMessage());
        }
    }

    private boolean isValidRequest(McpRequest request) {
        return request.getJsonrpc() != null 
                && request.getJsonrpc().equals("2.0")
                && request.getMethod() != null;
    }

    private McpResponse handleInitialize(McpRequest request) {
        log.info("Processing initialize request");
        
        Map<String, Object> result = Map.of(
                "protocolVersion", "2024-11-05",
                "capabilities", Map.of(
                        "tools", Map.of(),
                        "resources", Map.of(),
                        "prompts", Map.of()
                ),
                "serverInfo", Map.of(
                        "name", "spring-ai-mcp-server",
                        "version", "1.0.0"
                )
        );
        
        return createSuccessResponse(request.getId(), result);
    }

    private McpResponse handleToolsList(McpRequest request) {
        log.debug("Listing available tools");
        
        var tools = toolRegistry.getAllTools().stream()
                .map(tool -> Map.of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "inputSchema", tool.getInputSchema()
                ))
                .toList();
        
        return createSuccessResponse(request.getId(), Map.of("tools", tools));
    }

    private McpResponse handleToolsCall(McpRequest request) {
        log.info("Executing tool call: id={}", request.getId());
        
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("name")) {
            return createErrorResponse(request.getId(), 
                    McpError.INVALID_PARAMS.getCode(),
                    "Missing 'name' parameter");
        }
        
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        try {
            var result = toolRegistry.execute(toolName, arguments);
            return createSuccessResponse(request.getId(), result);
        } catch (ToolNotFoundException e) {
            return createErrorResponse(request.getId(),
                    McpError.TOOL_NOT_FOUND.getCode(),
                    e.getMessage());
        } catch (ToolExecutionException e) {
            return createErrorResponse(request.getId(),
                    McpError.TOOL_EXECUTION_ERROR.getCode(),
                    e.getMessage());
        }
    }

    private McpResponse handleResourcesList(McpRequest request) {
        log.debug("Listing available resources");
        
        var resources = resourceRegistry.getAllResources().stream()
                .map(resource -> Map.of(
                        "uri", resource.getUri(),
                        "name", resource.getName(),
                        "description", resource.getDescription(),
                        "mimeType", resource.getMimeType()
                ))
                .toList();
        
        return createSuccessResponse(request.getId(), Map.of("resources", resources));
    }

    private McpResponse handleResourcesRead(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("uri")) {
            return createErrorResponse(request.getId(),
                    McpError.INVALID_PARAMS.getCode(),
                    "Missing 'uri' parameter");
        }
        
        String uri = (String) params.get("uri");
        
        try {
            var content = resourceRegistry.read(uri);
            return createSuccessResponse(request.getId(), content);
        } catch (ResourceNotFoundException e) {
            return createErrorResponse(request.getId(),
                    McpError.RESOURCE_NOT_FOUND.getCode(),
                    e.getMessage());
        }
    }

    private McpResponse handlePromptsList(McpRequest request) {
        log.debug("Listing available prompts");
        
        var prompts = promptRegistry.getAllPrompts().stream()
                .map(prompt -> Map.of(
                        "name", prompt.getName(),
                        "description", prompt.getDescription(),
                        "arguments", prompt.getArguments()
                ))
                .toList();
        
        return createSuccessResponse(request.getId(), Map.of("prompts", prompts));
    }

    private McpResponse handlePromptsGet(McpRequest request) {
        Map<String, Object> params = request.getParams();
        if (params == null || !params.containsKey("name")) {
            return createErrorResponse(request.getId(),
                    McpError.INVALID_PARAMS.getCode(),
                    "Missing 'name' parameter");
        }
        
        String name = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
        
        try {
            var result = promptRegistry.render(name, arguments);
            return createSuccessResponse(request.getId(), result);
        } catch (PromptNotFoundException e) {
            return createErrorResponse(request.getId(),
                    McpError.METHOD_NOT_FOUND.getCode(),
                    e.getMessage());
        }
    }

    private McpResponse handlePing(McpRequest request) {
        return createSuccessResponse(request.getId(), Map.of("pong", true));
    }

    private void handleInitialized(McpRequest request) {
        log.info("Client initialized connection");
    }

    private void handleShutdown(McpRequest request) {
        log.info("Client initiated shutdown");
    }

    private McpResponse createSuccessResponse(Object id, Object result) {
        return new McpResponse("2.0", result, id);
    }

    private McpResponse createErrorResponse(Object id, int code, String message) {
        return new McpResponse("2.0", 
                Map.of("code", code, "message", message), id);
    }

    private McpResponse createParseError(Object id) {
        return new McpResponse("2.0",
                Map.of("code", McpError.PARSE_ERROR.getCode(), 
                       "message", McpError.PARSE_ERROR.getMessage()),
                id);
    }

    private McpResponse createInvalidRequestError(McpRequest request) {
        return new McpResponse("2.0",
                Map.of("code", McpError.INVALID_REQUEST.getCode(),
                       "message", McpError.INVALID_REQUEST.getMessage()),
                request != null ? request.getId() : null);
    }

    private McpResponse createMethodNotFoundError(McpRequest request, String method) {
        return new McpResponse("2.0",
                Map.of("code", McpError.METHOD_NOT_FOUND.getCode(),
                       "message", "Method not found: " + method),
                request.getId());
    }

    private McpResponse createInternalError(McpRequest request, String message) {
        return new McpResponse("2.0",
                Map.of("code", McpError.INTERNAL_ERROR.getCode(),
                       "message", message),
                request != null ? request.getId() : null);
    }

    // Placeholder interfaces - would be defined in separate files
    public interface ToolRegistry {
        java.util.List<com.example.mcp.capabilities.tools.annotation.McpTool> getAllTools();
        Map<String, Object> execute(String name, Map<String, Object> arguments);
    }

    public interface ResourceRegistry {
        java.util.List<Resource> getAllResources();
        Map<String, Object> read(String uri);
    }

    public interface PromptRegistry {
        java.util.List<Prompt> getAllPrompts();
        Map<String, Object> render(String name, Map<String, Object> arguments);
    }

    public record Resource(String uri, String name, String description, String mimeType) {}
    public record Prompt(String name, String description, java.util.List<String> arguments) {}

    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String name) {
            super("Tool not found: " + name);
        }
    }

    public static class ToolExecutionException extends RuntimeException {
        public ToolExecutionException(String message) {
            super(message);
        }
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String uri) {
            super("Resource not found: " + uri);
        }
    }

    public static class PromptNotFoundException extends RuntimeException {
        public PromptNotFoundException(String name) {
            super("Prompt not found: " + name);
        }
    }
}
