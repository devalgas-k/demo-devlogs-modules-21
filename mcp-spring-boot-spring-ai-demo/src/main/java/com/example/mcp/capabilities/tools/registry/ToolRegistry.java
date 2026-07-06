package com.example.mcp.capabilities.tools.registry;

import com.example.mcp.capabilities.tools.annotation.McpTool;
import com.example.mcp.capabilities.tools.executor.AsyncMcpToolExecutor;
import com.example.mcp.capabilities.tools.executor.ToolExecutor;
import com.example.mcp.capabilities.tools.executor.ToolRequest;
import com.example.mcp.capabilities.tools.executor.ToolResponse;
import com.example.mcp.exception.ToolExecutionException;
import com.example.mcp.exception.ToolNotFoundException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Central registry for MCP tools.
 * Manages tool registration, discovery, and execution.
 * Supports:
 * <ul>
 *     <li>Automatic component scanning of tools annotated with {@link McpTool}</li>
 *     <li>Synchronous and asynchronous execution</li>
 *     <li>Category filtering</li>
 *     <li>Thread-safety via ConcurrentHashMap</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @Autowired
 * private ToolRegistry toolRegistry;
 * 
 * // Execute a tool
 * ToolResponse response = toolRegistry.execute("weather", 
 *     ToolRequest.of(Map.of("location", "Paris")));
 * 
 * // List tools by category
 * List<ToolDescriptor> databaseTools = toolRegistry.getToolsByCategory("database");
 * }</pre>
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final ApplicationContext applicationContext;
    private final ConcurrentHashMap<String, ToolEntry> tools = new ConcurrentHashMap<>();
    private final ExecutorService asyncExecutor;

    @Value("${mcp.tool.default-timeout:30s}")
    private Duration defaultTimeout;

    // Constructor with Spring context injection
    public ToolRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.asyncExecutor = Executors.newCachedThreadPool();
    }

    /**
     * Registry initialization via component scanning.
     * Called automatically after bean initialization,
     * scans all beans implementing {@link ToolExecutor} or {@link AsyncMcpToolExecutor}.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing ToolRegistry with component scanning...");

        Map<String, ToolExecutor> toolExecutors = applicationContext.getBeansOfType(ToolExecutor.class);
        Map<String, AsyncMcpToolExecutor> asyncToolExecutors = applicationContext.getBeansOfType(AsyncMcpToolExecutor.class);

        // Register sync tools
        toolExecutors.forEach((beanName, executor) -> {
            Optional<McpTool> annotation = findToolAnnotation(executor.getClass());
            if (annotation.isPresent()) {
                McpTool toolAnnotation = annotation.get();
                if (toolAnnotation.enabled()) {
                    register(new ToolEntry(
                        toolAnnotation.name(),
                        toolAnnotation.description(),
                        toolAnnotation.category(),
                        toolAnnotation.version(),
                        executor,
                        null,
                        toolAnnotation.timeoutSeconds() > 0
                            ? Duration.ofSeconds(toolAnnotation.timeoutSeconds())
                            : defaultTimeout
                    ));
                } else {
                    log.debug("Tool {} is disabled, skipping registration", toolAnnotation.name());
                }
            }
        });

        // Register async tools
        asyncToolExecutors.forEach((beanName, executor) -> {
            // Skip if already registered as sync tool
            if (toolExecutors.containsKey(beanName)) {
                return;
            }
            Optional<McpTool> annotation = findToolAnnotation(executor.getClass());
            if (annotation.isPresent()) {
                McpTool toolAnnotation = annotation.get();
                if (toolAnnotation.enabled()) {
                    register(new ToolEntry(
                        toolAnnotation.name(),
                        toolAnnotation.description(),
                        toolAnnotation.category(),
                        toolAnnotation.version(),
                        null,
                        executor,
                        toolAnnotation.timeoutSeconds() > 0
                            ? Duration.ofSeconds(toolAnnotation.timeoutSeconds())
                            : defaultTimeout
                    ));
                }
            }
        });

        log.info("ToolRegistry initialized with {} tools", tools.size());
    }

    /**
     * Find {@link McpTool} annotation on a class or its interfaces.
     */
    private Optional<McpTool> findToolAnnotation(Class<?> clazz) {
        McpTool annotation = clazz.getAnnotation(McpTool.class);
        if (annotation != null) {
            return Optional.of(annotation);
        }

        // Check interfaces (for proxies)
        for (Class<?> iface : clazz.getInterfaces()) {
            annotation = iface.getAnnotation(McpTool.class);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }

        // Check superclass
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return findToolAnnotation(superClass);
        }

        return Optional.empty();
    }

    /**
     * Register a tool in the registry.
     *
     * @param entry the tool entry to register
     */
    public void register(ToolEntry entry) {
        if (tools.containsKey(entry.name())) {
            log.warn("Tool {} is already registered. Replacing with new implementation.", entry.name());
        }
        tools.put(entry.name(), entry);
        log.debug("Registered tool: {} (category={}, version={})",
            entry.name(), entry.category(), entry.version());
    }

    /**
     * Execute a tool by name.
     *
     * @param toolName the tool name
     * @param request the request containing parameters
     * @return the tool response
     * @throws ToolNotFoundException if tool does not exist
     * @throws ToolExecutionException if execution fails
     */
    public ToolResponse execute(String toolName, ToolRequest request) {
        ToolEntry entry = tools.get(toolName);
        if (entry == null) {
            throw new ToolNotFoundException("Tool not found: " + toolName);
        }

        if (entry.syncExecutor() != null) {
            return entry.syncExecutor().execute(request);
        }

        // Use async executor with blocking get
        try {
            return entry.asyncExecutor()
                .orElseThrow(() -> new ToolExecutionException("No executor available for tool: " + toolName))
                .executeAsync(request)
                .get(entry.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            entry.asyncExecutor().ifPresent(AsyncMcpToolExecutor::cancel);
            throw new ToolExecutionException("Tool execution timed out: " + toolName);
        } catch (Exception e) {
            throw new ToolExecutionException("Tool execution failed: " + toolName, e);
        }
    }

    /**
     * Execute a tool asynchronously.
     *
     * @param toolName the tool name
     * @param request the request containing parameters
     * @return a CompletableFuture containing the response
     * @throws ToolNotFoundException if tool does not exist
     */
    public java.util.concurrent.CompletableFuture<ToolResponse> executeAsync(String toolName, ToolRequest request) {
        ToolEntry entry = tools.get(toolName);
        if (entry == null) {
            throw new ToolNotFoundException("Tool not found: " + toolName);
        }

        AsyncMcpToolExecutor executor = entry.asyncExecutor()
            .orElseThrow(() -> new ToolExecutionException("Tool does not support async execution: " + toolName));

        return executor.executeAsync(request)
            .orTimeout(entry.timeout().toMillis(), TimeUnit.MILLISECONDS)
            .exceptionally(ex -> {
                if (ex.getCause() instanceof TimeoutException) {
                    executor.cancel();
                    return ToolResponse.error(-32002, "Tool execution timed out: " + toolName);
                }
                return ToolResponse.error(-32002, "Tool execution failed: " + ex.getMessage());
            });
    }

    /**
     * Get a tool by name.
     *
     * @param toolName the tool name
     * @return an Optional containing the tool entry if found
     */
    public Optional<ToolEntry> getTool(String toolName) {
        return Optional.ofNullable(tools.get(toolName));
    }

    /**
     * Get all registered tools.
     *
     * @return a collection of all tool entries
     */
    public Collection<ToolEntry> getAllTools() {
        return tools.values();
    }

    /**
     * Get tools by category.
     *
     * @param category the category to search
     * @return a list of tools in the category
     */
    public List<ToolEntry> getToolsByCategory(String category) {
        return tools.values().stream()
            .filter(entry -> entry.category().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }

    /**
     * Filter tools according to a predicate.
     *
     * @param predicate the filter predicate
     * @return a list of matching tools
     */
    public List<ToolEntry> findTools(Predicate<ToolEntry> predicate) {
        return tools.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * Get names of all available categories.
     *
     * @return a collection of category names
     */
    public Collection<String> getCategories() {
        return tools.values().stream()
            .map(ToolEntry::category)
            .collect(Collectors.toSet());
    }

    /**
     * Remove a tool from the registry.
     *
     * @param toolName the name of the tool to remove
     * @return true if the tool was removed, false otherwise
     */
    public boolean unregister(String toolName) {
        return tools.remove(toolName) != null;
    }

    /**
     * Check if a tool exists.
     *
     * @param toolName the tool name
     * @return true if the tool exists
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * Return the number of registered tools.
     *
     * @return the tool count
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * Entry record representing a registered tool.
     *
     * @param name the unique tool name
     * @param description the tool description
     * @param category the tool category
     * @param version the tool version
     * @param syncExecutor the sync executor (can be null)
     * @param asyncExecutor the async executor (can be null)
     * @param timeout the execution timeout
     */
    public record ToolEntry(
        String name,
        String description,
        String category,
        String version,
        ToolExecutor syncExecutor,
        AsyncMcpToolExecutor asyncExecutor,
        Duration timeout
    ) {
        public boolean isAsync() {
            return asyncExecutor != null;
        }

        public boolean isSync() {
            return syncExecutor != null;
        }
    }
}
