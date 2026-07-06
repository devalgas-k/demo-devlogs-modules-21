package com.example.mcp.capabilities.tools.executor;

import com.example.mcp.capabilities.tools.annotation.McpTool;
import com.example.mcp.capabilities.tools.response.ToolResponse;
import com.example.mcp.capabilities.tools.response.ToolResponse.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Weather lookup tool with validation, error handling and async support.
 */
@Component
public class WeatherTool implements ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    @McpTool(name = "weather", description = "Get current weather information for a location")
    public ToolResponse execute(
            @NotBlank(message = "Location is required") String location,
            @Pattern(regexp = "(celsius|fahrenheit)", flags = Pattern.Flag.IGNORE_CASE, 
                     message = "Unit must be 'celsius' or 'fahrenheit'") String unit) {
        
        long startTime = System.currentTimeMillis();
        String correlationId = generateCorrelationId();
        
        log.info("[{}] Weather request received for location: {}", correlationId, 
                 maskLocation(location));
        
        try {
            validateLocation(location);
            
            // Mock weather data - replace with actual API call
            Map<String, Object> weatherData = fetchWeatherData(location, 
                    normalizeUnit(unit));
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[{}] Weather request completed successfully in {}ms", 
                    correlationId, duration);
            
            return ToolResponse.success(weatherData);
            
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Validation error: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (WeatherServiceException e) {
            log.error("[{}] Weather service error: {}", correlationId, e.getMessage());
            return ToolResponse.error(ErrorCode.SERVICE_UNAVAILABLE, 
                    "Weather service temporarily unavailable");
        } catch (Exception e) {
            log.error("[{}] Unexpected error during weather lookup", correlationId, e);
            return ToolResponse.error(ErrorCode.INTERNAL_ERROR, 
                    "An unexpected error occurred");
        }
    }
    
    @Async
    public CompletableFuture<ToolResponse> executeAsync(String location, String unit) {
        return CompletableFuture.completedFuture(execute(location, unit));
    }
    
    private void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("Location cannot be empty");
        }
        if (location.length() > 200) {
            throw new IllegalArgumentException("Location name too long");
        }
        // Basic pattern check - alphanumeric, spaces, commas, hyphens
        if (!location.matches("^[a-zA-Z0-9\\s,\\-'.]+$")) {
            throw new IllegalArgumentException("Location contains invalid characters");
        }
    }
    
    private String normalizeUnit(String unit) {
        if (unit == null || unit.isEmpty()) {
            return "celsius";
        }
        return unit.toLowerCase();
    }
    
    private Map<String, Object> fetchWeatherData(String location, String unit) {
        // Mock implementation - replace with actual weather API integration
        // Example: OpenWeatherMap, WeatherAPI, etc.
        return Map.of(
            "location", location,
            "temperature", unit.equals("celsius") ? 22 : 72,
            "unit", unit,
            "condition", "partly_cloudy",
            "humidity", 65,
            "windSpeed", 12,
            "timestamp", ZonedDateTime.now().toString(),
            "forecast", java.util.List.of(
                Map.of("day", "today", "high", 25, "low", 18),
                Map.of("day", "tomorrow", "high", 27, "low", 19)
            )
        );
    }
    
    private String maskLocation(String location) {
        // Mask potentially sensitive location data for logging
        if (location.length() <= 4) {
            return "***";
        }
        return location.substring(0, 2) + "***" + location.substring(location.length() - 2);
    }
    
    private String generateCorrelationId() {
        return "wth-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString((int)(Math.random() * 0xFFFF));
    }
    
    public interface WeatherServiceException extends RuntimeException {
        WeatherServiceException withCode(String code);
    }
}
