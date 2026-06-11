package com.example.demo.security;

import com.example.demo.security.jwt.RotatingRsaJwtDecoder;
import com.example.demo.security.opaque.CachingOpaqueTokenIntrospector;
import com.example.demo.security.opaque.SlowOpaqueTokenIntrospector;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(DemoSecurityProperties.class)
public class DemoSecurityConfig {

    @Bean
    @Profile({"mode-a", "mode-a-cached"})
    SecurityFilterChain securityFilterChainOpaque(HttpSecurity http, OpaqueTokenIntrospector introspector) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/actuator/**", "/api/public").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.opaqueToken(opaque -> opaque.introspector(introspector)))
                .build();
    }

    @Bean
    @Profile("mode-b")
    SecurityFilterChain securityFilterChainJwt(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/actuator/**", "/api/public").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        scopes.setAuthorityPrefix("SCOPE_");
        scopes.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(scopes);
        return converter;
    }

    @Bean
    @Profile("mode-a")
    OpaqueTokenIntrospector opaqueTokenIntrospector(DemoSecurityProperties properties, SecurityStats stats) {
        return new SlowOpaqueTokenIntrospector(Duration.ofMillis(properties.introspectionDelayMs()), stats);
    }

    @Bean
    @Profile("mode-a-cached")
    OpaqueTokenIntrospector cachedOpaqueTokenIntrospector(DemoSecurityProperties properties, SecurityStats stats) {
        SlowOpaqueTokenIntrospector slow = new SlowOpaqueTokenIntrospector(Duration.ofMillis(properties.introspectionDelayMs()), stats);
        long ttlMs = Math.max(1, properties.introspectionCacheTtlMs());
        return new CachingOpaqueTokenIntrospector(slow, Duration.ofMillis(ttlMs), 10_000);
    }

    @Bean
    @Profile("mode-b")
    JwtDecoder jwtDecoder(DemoSecurityProperties properties, KeyRing keyRing, SecurityStats stats) {
        RotatingRsaJwtDecoder decoder = new RotatingRsaJwtDecoder(keyRing, stats);
        long skewSeconds = Math.max(0, properties.clockSkewSeconds());
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ofSeconds(skewSeconds));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtIssuerValidator(properties.issuer()), timestamp));
        decoder.setAudience(properties.audience());
        return decoder;
    }
}
