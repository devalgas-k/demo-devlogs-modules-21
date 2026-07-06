package com.example.mcp.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

/**
 * Resilience4j configuration.
 * Defines circuit breakers, retry policies and bulkhead for LLM calls.
 */
@Configuration
public class ResilienceConfig {

    // ==========================================================================
    // Circuit Breaker Settings
    // ==========================================================================

    @Value("${resilience4j.circuitbreaker.configs.default.sliding-window-size:10}")
    private int cbSlidingWindowSize;

    @Value("${resilience4j.circuitbreaker.configs.default.failure-rate-threshold:50}")
    private int cbFailureRateThreshold;

    @Value("${resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state:30s}")
    private Duration cbWaitDuration;

    @Value("${resilience4j.circuitbreaker.configs.default.permitted-number-of-calls-in-half-open-state:3}")
    private int cbPermittedCalls;

    @Value("${resilience4j.circuitbreaker.configs.default.slow-call-duration-threshold:10s}")
    private Duration cbSlowCallThreshold;

    @Value("${resilience4j.circuitbreaker.configs.default.slow-call-rate-threshold:80}")
    private int cbSlowCallRateThreshold;

    // ==========================================================================
    // Retry Settings
    // ==========================================================================

    @Value("${resilience4j.retry.configs.default.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${resilience4j.retry.configs.default.wait-duration:1s}")
    private Duration retryWaitDuration;

    @Value("${resilience4j.retry.configs.default.enable-exponential-backoff:true}")
    private boolean retryExponentialBackoff;

    // ==========================================================================
    // Circuit Breaker Registry
    // ==========================================================================

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.builder()
                // Sliding window configuration
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cbSlidingWindowSize)
                
                // Failure threshold (50% failures = open)
                .failureRateThreshold(cbFailureRateThreshold)
                
                // Wait duration before half-open transition
                .waitDurationInOpenState(cbWaitDuration)
                
                // Number of calls allowed in half-open state
                .permittedNumberOfCallsInHalfOpenState(cbPermittedCalls)
                
                // Slow call thresholds
                .slowCallDurationThreshold(cbSlowCallThreshold)
                .slowCallRateThreshold(cbSlowCallRateThreshold)
                
                // Exceptions to record as failures
                .recordExceptions(IOException.class, RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                
                // Automatic state transition
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    /**
     * Circuit breaker for OpenAI.
     */
    @Bean
    public CircuitBreaker openAiCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig openAiConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(15))
                .slowCallRateThreshold(80)
                .build();
        
        return registry.circuitBreaker("openai", openAiConfig);
    }

    /**
     * Circuit breaker for Anthropic.
     */
    @Bean
    public CircuitBreaker anthropicCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig anthropicConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slowCallDurationThreshold(Duration.ofSeconds(15))
                .slowCallRateThreshold(80)
                .build();
        
        return registry.circuitBreaker("anthropic", anthropicConfig);
    }

    /**
     * Circuit breaker for Ollama (local, less likely to fail).
     */
    @Bean
    public CircuitBreaker ollamaCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig ollamaConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .failureRateThreshold(70) // More tolerant for local
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slowCallDurationThreshold(Duration.ofSeconds(30))
                .slowCallRateThreshold(90)
                .build();
        
        return registry.circuitBreaker("ollama", ollamaConfig);
    }

    // ==========================================================================
    // Retry Registry
    // ==========================================================================

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(retryMaxAttempts)
                .waitDuration(retryWaitDuration)
                .retryExceptions(IOException.class, RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    /**
     * Retry policy for LLM calls with exponential backoff.
     */
    @Bean
    public Retry llmRetryPolicy(RetryRegistry registry) {
        RetryConfig llmRetryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(IOException.class, RuntimeException.class)
                // Exponential backoff: 1s, 2s, 4s
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofSeconds(1).toMillis(),
                        2.0
                ))
                .failAfterMaxAttempts(true)
                .build();

        return registry.retry("llmRetry", llmRetryConfig);
    }

    /**
     * Retry policy for external services.
     */
    @Bean
    public Retry externalServiceRetryPolicy(RetryRegistry registry) {
        RetryConfig externalRetryConfig = RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(IOException.class)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2.0))
                .failAfterMaxAttempts(true)
                .build();

        return registry.retry("externalServiceRetry", externalRetryConfig);
    }

    // ==========================================================================
    // Bulkhead Configuration (use with Resilience4j Bulkhead)
    // ==========================================================================

    /**
     * Bulkhead configuration to limit parallel calls.
     */
    @Bean
    public BulkheadConfigProvider bulkheadConfigProvider() {
        return new BulkheadConfigProvider(
                10,  // maxConcurrentCalls - max simultaneous calls
                20   // maxWaitDuration - max wait time for acquire call
        );
    }

    public record BulkheadConfigProvider(int maxConcurrentCalls, int maxWaitDuration) {
        public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
        public int getMaxWaitDuration() { return maxWaitDuration; }
    }

    // ==========================================================================
    // Configurers Helpers
    // ==========================================================================

    /**
     * IntervalFunction for exponential backoff.
     */
    public static class IntervalFunction {
        private final java.util.function.LongUnaryOperator function;

        private IntervalFunction(java.util.function.LongUnaryOperator function) {
            this.function = function;
        }

        public static IntervalFunction ofExponentialBackoff(long initialIntervalMillis, double multiplier) {
            return new IntervalFunction(attempt -> {
                long interval = (long) (initialIntervalMillis * Math.pow(multiplier, attempt - 1));
                // Maximum 30 seconds between retries
                return Math.min(interval, 30_000);
            });
        }

        public long apply(long attempt) {
            return function.applyAsLong(attempt);
        }
    }
}
