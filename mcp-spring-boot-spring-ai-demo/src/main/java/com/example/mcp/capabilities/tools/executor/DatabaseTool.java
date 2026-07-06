package com.example.mcp.capabilities.tools.executor;

import com.example.mcp.capabilities.tools.annotation.McpTool;
import com.example.mcp.capabilities.tools.response.ToolResponse;
import com.example.mcp.capabilities.tools.response.ToolResponse.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Database query tool with SQL injection prevention and connection pooling.
 * This tool only supports read-only queries (SELECT statements).
 */
@Component
public class DatabaseTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseTool.class);
    
    // SQL injection prevention patterns
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
        ".*(DROP|DELETE|INSERT|UPDATE|TRUNCATE|ALTER|CREATE|EXEC|EXECUTE|" +
        "GRANT|REVOKE|MERGE|CALL|INTO|OUTFILE|DUMPFILE).*", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern UNION_BASED_INJECTION = Pattern.compile(
        ".*(UNION\\s+(ALL\\s+)?SELECT|UNION\\s+SELECT).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMMENT_INJECTION = Pattern.compile(
        ".*(/\\*|\\*/|--|;).*"
    );
    
    private static final int DEFAULT_MAX_ROWS = 100;
    private static final int MAX_ALLOWED_ROWS = 1000;
    
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    
    // Connection pool metrics (thread-safe)
    private final ConcurrentHashMap<String, ConnectionMetrics> connectionMetrics = 
            new ConcurrentHashMap<>();

    @Autowired
    public DatabaseTool(DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }
    
    @McpTool(name = "database", description = "Execute read-only SQL queries")
    public ToolResponse execute(
            @NotBlank(message = "Query is required") String query,
            @Positive(message = "maxRows must be positive") Integer maxRows) {
        
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        
        log.info("[{}] Database query request received", correlationId);
        log.debug("[{}] Query length: {} characters", correlationId, 
                query != null ? query.length() : 0);
        
        try {
            int effectiveMaxRows = normalizeMaxRows(maxRows);
            String sanitizedQuery = sanitizeQuery(query);
            
            validateQuery(sanitizedQuery);
            
            // Track connection usage
            String poolKey = getPoolKey();
            ConnectionMetrics metrics = connectionMetrics.computeIfAbsent(
                    poolKey, k -> new ConnectionMetrics());
            metrics.incrementRequestCount();
            
            // Execute query
            List<Map<String, Object>> results = executeReadQuery(
                    sanitizedQuery, effectiveMaxRows, correlationId);
            
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordQueryTime(duration);
            
            log.info("[{}] Query executed successfully, returned {} rows in {}ms", 
                    correlationId, results.size(), duration);
            
            return ToolResponse.success(Map.of(
                "rows", results,
                "rowCount", results.size(),
                "maxRows", effectiveMaxRows,
                "executionTimeMs", duration
            ));
            
        } catch (QueryValidationException e) {
            log.warn("[{}] Query validation failed: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (QueryExecutionException e) {
            log.error("[{}] Query execution failed: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.SERVICE_UNAVAILABLE, 
                    "Database query failed");
        } catch (Exception e) {
            log.error("[{}] Unexpected error during query execution", correlationId, e);
            return ToolResponse.error(ErrorCode.INTERNAL_ERROR, 
                    "An unexpected error occurred");
        }
    }
    
    private String sanitizeQuery(String query) {
        if (query == null) {
            throw new QueryValidationException("Query cannot be null");
        }
        // Normalize whitespace
        return query.trim().replaceAll("\\s+", " ");
    }
    
    private void validateQuery(String query) {
        if (query.isEmpty()) {
            throw new QueryValidationException("Query cannot be empty");
        }
        
        // Must start with SELECT (with optional whitespace)
        if (!query.toUpperCase().matches("^\\s*SELECT\\s+.*")) {
            throw new QueryValidationException(
                    "Only SELECT queries are allowed");
        }
        
        // Check for dangerous SQL patterns
        if (DANGEROUS_SQL_PATTERN.matcher(query).matches()) {
            throw new QueryValidationException(
                    "Query contains forbidden SQL operations");
        }
        
        // Check for UNION-based injection
        if (UNION_BASED_INJECTION.matcher(query).matches()) {
            throw new QueryValidationException(
                    "Query contains potentially dangerous UNION pattern");
        }
        
        // Check for comment injection
        if (COMMENT_INJECTION.matcher(query).matches()) {
            throw new QueryValidationException(
                    "Query contains forbidden comment patterns");
        }
        
        // Additional heuristic checks
        if (query.contains("'") && query.contains("--")) {
            throw new QueryValidationException(
                    "Query contains suspicious character combination");
        }
    }
    
    private int normalizeMaxRows(Integer maxRows) {
        if (maxRows == null || maxRows <= 0) {
            return DEFAULT_MAX_ROWS;
        }
        return Math.min(maxRows, MAX_ALLOWED_ROWS);
    }
    
    private List<Map<String, Object>> executeReadQuery(
            String query, int maxRows, String correlationId) {
        
        try {
            return jdbcTemplate.queryForList(
                    query + " LIMIT " + maxRows);
        } catch (Exception e) {
            log.error("[{}] JDBC query execution failed", correlationId, e);
            throw new QueryExecutionException("Failed to execute query: " + 
                    e.getMessage());
        }
    }
    
    private String getPoolKey() {
        return "pool-" + Thread.currentThread().getName();
    }
    
    public Map<String, Object> getPoolMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Map<String, ConnectionMetrics> snapshot = 
                new HashMap<>(connectionMetrics);
        
        metrics.put("trackedPools", snapshot.size());
        metrics.put("pools", snapshot.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toMap()
                )));
        
        return metrics;
    }
    
    private String generateCorrelationId() {
        return "db-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    // Inner class for connection metrics
    private static class ConnectionMetrics {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong totalQueryTime = new AtomicLong(0);
        private volatile long lastQueryTime = 0;
        
        void incrementRequestCount() {
            requestCount.incrementAndGet();
        }
        
        void recordQueryTime(long timeMs) {
            totalQueryTime.addAndGet(timeMs);
            lastQueryTime = timeMs;
        }
        
        Map<String, Object> toMap() {
            return Map.of(
                "requestCount", requestCount.get(),
                "totalQueryTimeMs", totalQueryTime.get(),
                "lastQueryTimeMs", lastQueryTime,
                "avgQueryTimeMs", requestCount.get() > 0 ? 
                        totalQueryTime.get() / requestCount.get() : 0
            );
        }
    }
    
    // Custom exceptions
    private static class QueryValidationException extends RuntimeException {
        QueryValidationException(String message) {
            super(message);
        }
    }
    
    private static class QueryExecutionException extends RuntimeException {
        QueryExecutionException(String message) {
            super(message);
        }
    }
}
