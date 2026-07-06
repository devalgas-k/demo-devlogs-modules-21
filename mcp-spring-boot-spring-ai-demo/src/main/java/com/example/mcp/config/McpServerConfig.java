package com.example.mcp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MCP server configuration.
 * Defines server parameters, timeouts, and session management.
 */
@Configuration
public class McpServerConfig {

    // ==========================================================================
    // Server Settings
    // ==========================================================================

    @Value("${mcp.server.name:spring-ai-mcp-server}")
    private String serverName;

    @Value("${mcp.server.version:1.0.0}")
    private String serverVersion;

    @Value("${mcp.transport:http-sse}")
    private String transport;

    // ==========================================================================
    // Tool Timeouts
    // ==========================================================================

    @Value("${mcp.tool.default-timeout:30s}")
    private Duration defaultToolTimeout;

    @Value("${mcp.tool.max-timeout:120s}")
    private Duration maxToolTimeout;

    @Value("${mcp.tool.executor-pool-size:10}")
    private int toolExecutorPoolSize;

    // ==========================================================================
    // Session Settings
    // ==========================================================================

    @Value("${mcp.session.timeout:10m}")
    private Duration sessionTimeout;

    @Value("${mcp.session.max-count:1000}")
    private int maxSessionCount;

    @Value("${mcp.session.cleanup-interval:5m}")
    private Duration sessionCleanupInterval;

    // ==========================================================================
    // SSE Settings
    // ==========================================================================

    @Value("${mcp.sse.heartbeat-interval:30s}")
    private Duration heartbeatInterval;

    @Value("${mcp.sse.max-connections:1000}")
    private int maxSseConnections;

    @Value("${mcp.sse.connection-timeout:5m}")
    private Duration sseConnectionTimeout;

    // ==========================================================================
    // Server Info Beans
    // ==========================================================================

    /**
     * MCP server information.
     */
    @Bean
    public McpServerInfo mcpServerInfo() {
        return new McpServerInfo(serverName, serverVersion, transport);
    }

    /**
     * Record containing server information.
     */
    public record McpServerInfo(String name, String version, String transport) {
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getTransport() { return transport; }
    }

    // ==========================================================================
    // Tool Executor Pool
    // ==========================================================================

    /**
     * Executor pool for tools.
     * Sized according to expected parallel tool count.
     */
    @Bean
    public ExecutorService toolExecutorService() {
        return Executors.newFixedThreadPool(toolExecutorPoolSize, r -> {
            Thread t = new Thread(r, "tool-executor");
            t.setDaemon(true);
            return t;
        });
    }

    // ==========================================================================
    // Session Cleanup Scheduler
    // ==========================================================================

    /**
     * Scheduler for periodic orphaned session cleanup.
     */
    @Bean
    public ScheduledExecutorService sessionCleanupScheduler() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "session-cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule periodic cleanup
        scheduler.scheduleAtFixedRate(
                () -> { /* Session cleanup logic called via SessionManager */ },
                sessionCleanupInterval.toMinutes(),
                sessionCleanupInterval.toMinutes(),
                TimeUnit.MINUTES
        );
        
        return scheduler;
    }

    // ==========================================================================
    // Timeout Configuration
    // ==========================================================================

    /**
     * Tool execution timeout configuration.
     */
    @Bean
    public ToolTimeoutConfig toolTimeoutConfig() {
        return new ToolTimeoutConfig(defaultToolTimeout, maxToolTimeout);
    }

    /**
     * Record containing timeout configuration.
     */
    public record ToolTimeoutConfig(Duration defaultTimeout, Duration maxTimeout) {
        public Duration getDefaultTimeout() { return defaultTimeout; }
        public Duration getMaxTimeout() { return maxTimeout; }
        
        /**
         * Calculate effective timeout for a tool.
         * Cannot exceed maxTimeout.
         */
        public Duration effectiveTimeout(Duration requested) {
            if (requested == null) {
                return defaultTimeout;
            }
            return requested.compareTo(maxTimeout) > 0 ? maxTimeout : requested;
        }
    }

    // ==========================================================================
    // SSE Configuration
    // ==========================================================================

    /**
     * SSE configuration for incoming connections.
     */
    @Bean
    public SseConfig sseConfig() {
        return new SseConfig(heartbeatInterval, maxSseConnections, sseConnectionTimeout);
    }

    /**
     * Record containing SSE configuration.
     */
    public record SseConfig(
            Duration heartbeatInterval,
            int maxConnections,
            Duration connectionTimeout
    ) {
        public Duration getHeartbeatInterval() { return heartbeatInterval; }
        public int getMaxConnections() { return maxConnections; }
        public Duration getConnectionTimeout() { return connectionTimeout; }
    }
}
