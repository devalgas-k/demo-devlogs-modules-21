package com.example.mcp.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Security configuration.
 * Includes HTTPS, CORS, and rate limiting with Bucket4j.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ==========================================================================
    // Rate Limiting Settings
    // ==========================================================================

    @Value("${mcp.security.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${mcp.security.rate-limit.burst-capacity:20}")
    private int burstCapacity;

    // ==========================================================================
    // CORS Settings
    // ==========================================================================

    @Value("${mcp.security.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Value("${mcp.security.cors.allowed-methods:GET,POST,OPTIONS}")
    private List<String> allowedMethods;

    @Value("${mcp.security.cors.max-age:3600}")
    private Duration corsMaxAge;

    // ==========================================================================
    // Security Filter Chain
    // ==========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (MCP uses JSON-RPC, not forms)
            .csrf(csrf -> csrf.disable())
            
            // Stateless session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // HTTP security policy
            .securityMatcher("/mcp/**")
            
            // Allow MCP endpoints
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/mcp/health").permitAll()
                .requestMatchers("/mcp/stream").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .anyRequest().authenticated()
            )
            
            // Add rate limiting filter
            .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    // ==========================================================================
    // Rate Limiting Filter
    // ==========================================================================

    /**
     * Rate limiting filter by IP using Bucket4j.
     */
    @Bean
    public OncePerRequestFilter rateLimitFilter() {
        return new OncePerRequestFilter() {
            // Bucket per IP address
            private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                
                String clientIp = getClientIp(request);
                Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

                if (bucket.tryConsume(1)) {
                    // Add rate limit headers
                    response.addHeader("X-RateLimit-Remaining", 
                            String.valueOf(bucket.getAvailableTokens()));
                    filterChain.doFilter(request, response);
                } else {
                    // Rate limit exceeded
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"error\":\"Rate limit exceeded\",\"retryAfter\":60}"
                    );
                    response.addHeader("Retry-After", "60");
                }
            }

            private Bucket createBucket(String key) {
                // Greedy refill: 100 requests per minute
                Bandwidth limit = Bandwidth.classic(
                        requestsPerMinute,
                        Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
                );
                return Bucket.builder()
                        .addLimit(limit)
                        .build();
            }

            private String getClientIp(HttpServletRequest request) {
                // Extract real IP (handles proxies)
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                return request.getRemoteAddr();
            }
        };
    }

    // ==========================================================================
    // CORS Configuration
    // ==========================================================================

    /**
     * CORS configuration for MCP clients (Claude Desktop, etc.).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            // Default: allow common origins in development
            configuration.setAllowedOriginPatterns(List.of(
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    " claude-desktop://*"
            ));
        } else {
            configuration.setAllowedOrigins(allowedOrigins);
        }
        
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(List.of(
                "X-RateLimit-Remaining",
                "X-Request-Id",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));
        configuration.setMaxAge(corsMaxAge.getSeconds());
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/mcp/**", configuration);
        return source;
    }

    // ==========================================================================
    // Connection Counter
    // ==========================================================================

    /**
     * Active connections counter for monitoring.
     */
    @Bean
    public AtomicInteger activeConnections() {
        return new AtomicInteger(0);
    }
}
