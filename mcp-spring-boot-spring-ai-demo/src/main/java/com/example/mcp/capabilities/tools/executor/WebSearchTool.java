package com.example.mcp.capabilities.tools.executor;

import com.example.mcp.capabilities.tools.annotation.McpTool;
import com.example.mcp.capabilities.tools.response.ToolResponse;
import com.example.mcp.capabilities.tools.response.ToolResponse.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Web search tool with external API call support, async execution,
 * and comprehensive error handling for API failures.
 */
@Component
public class WebSearchTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    
    private final WebClient webClient;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    
    // Mock API configuration - replace with actual search API
    private static final String SEARCH_API_BASE_URL = "https://api.example.com/search";
    private static final String SEARCH_API_KEY = "demo-api-key"; // Load from config
    
    public WebSearchTool() {
        this.webClient = WebClient.builder()
                .baseUrl(SEARCH_API_BASE_URL)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Spring-AI-MCP-Server/1.0")
                .build();
    }
    
    @McpTool(name = "websearch", description = "Search the web for information")
    public ToolResponse execute(
            @NotBlank(message = "Query is required") String query,
            @Positive(message = "Limit must be positive") Integer limit) {
        
        String correlationId = generateCorrelationId();
        long startTime = System.currentTimeMillis();
        
        log.info("[{}] Web search request received", correlationId);
        log.debug("[{}] Query: {}, requested limit: {}", 
                correlationId, maskQuery(query), limit);
        
        try {
            int effectiveLimit = normalizeLimit(limit);
            validateQuery(query);
            
            // Perform synchronous search (blocking)
            SearchResult result = performSearch(query, effectiveLimit, correlationId);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] Search completed successfully in {}ms, {} results", 
                    correlationId, duration, result.results().size());
            
            return ToolResponse.success(Map.of(
                    "query", query,
                    "results", result.results(),
                    "totalResults", result.totalResults(),
                    "limit", effectiveLimit,
                    "executionTimeMs", duration,
                    "searchEngine", result.source()
            ));
            
        } catch (QueryValidationException e) {
            log.warn("[{}] Query validation failed: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (SearchApiException e) {
            log.error("[{}] Search API error: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.SERVICE_UNAVAILABLE, 
                    "Search service temporarily unavailable");
        } catch (Exception e) {
            log.error("[{}] Unexpected error during search", correlationId, e);
            return ToolResponse.error(ErrorCode.INTERNAL_ERROR, 
                    "An unexpected error occurred");
        }
    }
    
    /**
     * Async search execution using reactive WebClient.
     */
    public CompletableFuture<ToolResponse> executeAsync(String query, Integer limit) {
        String correlationId = generateCorrelationId();
        
        log.info("[{}] Async web search requested", correlationId);
        
        return CompletableFuture.supplyAsync(() -> execute(query, limit));
    }
    
    /**
     * Reactive search with timeout and error handling.
     */
    public Mono<ToolResponse> executeReactive(String query, Integer limit) {
        String correlationId = generateCorrelationId();
        
        log.info("[{}] Reactive web search requested", correlationId);
        
        int effectiveLimit = normalizeLimit(limit);
        
        return Mono.fromCallable(() -> {
                    validateQuery(query);
                    return performSearch(query, effectiveLimit, correlationId);
                })
                .timeout(TIMEOUT)
                .map(result -> ToolResponse.success(Map.of(
                        "query", query,
                        "results", result.results(),
                        "totalResults", result.totalResults(),
                        "limit", effectiveLimit,
                        "searchEngine", result.source()
                )))
                .onErrorResume(e -> {
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        log.error("[{}] Search timed out after {}ms", 
                                correlationId, TIMEOUT.toMillis());
                        return Mono.just(ToolResponse.error(
                                ErrorCode.TIMEOUT, "Search request timed out"));
                    }
                    if (e instanceof SearchApiException) {
                        log.error("[{}] Search API error", correlationId, e);
                        return Mono.just(ToolResponse.error(
                                ErrorCode.SERVICE_UNAVAILABLE, 
                                "Search service temporarily unavailable"));
                    }
                    log.error("[{}] Unexpected error during reactive search", 
                            correlationId, e);
                    return Mono.just(ToolResponse.error(
                            ErrorCode.INTERNAL_ERROR, 
                            "An unexpected error occurred"));
                });
    }
    
    private void validateQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new QueryValidationException("Query cannot be empty");
        }
        if (query.trim().length() < 2) {
            throw new QueryValidationException(
                    "Query must be at least 2 characters");
        }
        if (query.length() > 500) {
            throw new QueryValidationException(
                    "Query is too long (max 500 characters)");
        }
    }
    
    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
    
    private SearchResult performSearch(
            String query, int limit, String correlationId) {
        
        try {
            // Mock implementation - replace with actual API call
            // Example: Google Custom Search API, Bing Search API, DuckDuckGo API
            
            // Simulated search results
            List<Map<String, String>> mockResults = generateMockResults(
                    query, limit);
            
            return new SearchResult(
                    mockResults,
                    mockResults.size() * 10,
                    "mock-search-api"
            );
            
            /* 
            // Real API call example (commented out):
            SearchResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("q", query)
                            .queryParam("limit", limit)
                            .build())
                    .header("Authorization", "Bearer " + SEARCH_API_KEY)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> 
                            Mono.error(new SearchApiException(
                                    "API returned status: " + 
                                    clientResponse.statusCode())))
                    .bodyToMono(SearchResponse.class)
                    .block(TIMEOUT);
            
            return new SearchResult(
                    results: response.items(),
                    totalResults: response.totalHits(),
                    source: response.searchEngine()
            );
            */
            
        } catch (Exception e) {
            if (e instanceof SearchApiException) {
                throw e;
            }
            throw new SearchApiException("Search failed: " + e.getMessage());
        }
    }
    
    private List<Map<String, String>> generateMockResults(String query, int limit) {
        return java.util.stream.IntStream.range(0, Math.min(limit, 10))
                .mapToObj(i -> Map.of(
                        "title", "Result " + (i + 1) + " for: " + 
                                query.substring(0, Math.min(query.length(), 50)),
                        "url", "https://example.com/result/" + (i + 1),
                        "snippet", "This is a mock search result snippet for '" + 
                                query + "'. It contains relevant information...",
                        "rank", String.valueOf(i + 1)
                ))
                .toList();
    }
    
    private String maskQuery(String query) {
        // Mask sensitive queries for logging
        if (query == null || query.length() <= 4) {
            return "***";
        }
        return query.substring(0, Math.min(20, query.length() / 2)) + "***";
    }
    
    private String generateCorrelationId() {
        return "ws-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    // Inner record for search results
    private record SearchResult(
            List<Map<String, String>> results,
            int totalResults,
            String source
    ) {}
    
    // Custom exceptions
    private static class QueryValidationException extends RuntimeException {
        QueryValidationException(String message) {
            super(message);
        }
    }
    
    private static class SearchApiException extends RuntimeException {
        SearchApiException(String message) {
            super(message);
        }
    }
}
