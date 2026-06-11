package com.example.demo;

import com.example.demo.security.SecurityStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("mode-b")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModeBJwtValidationFailuresTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    SecurityStats stats;

    @Test
    void missingScopeYields403() {
        String token = rest.getForObject("/auth/jwt?sub=alice&scope=admin", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange("/api/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void invalidAudienceYields401() {
        String token = rest.getForObject("/auth/jwt?sub=alice&scope=read&aud=api://wrong", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange("/api/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void invalidIssuerYields401() {
        String token = rest.getForObject("/auth/jwt?sub=alice&scope=read&iss=https://issuer.wrong.example", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange("/api/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void expiredTokenYields401() {
        String token = rest.getForObject("/auth/jwt?sub=alice&scope=read&ttlSeconds=10&iatSecondsAgo=3600", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<String> response = rest.exchange("/api/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void tamperedTokenYields401AndCountsSignatureFailure() {
        String token = rest.getForObject("/auth/jwt?sub=alice&scope=read", String.class);
        String tampered = token.substring(0, token.length() - 2) + "aa";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tampered);

        ResponseEntity<String> response = rest.exchange("/api/secure", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);

        assertThat(stats.snapshot().jwtSignatureFailures()).isGreaterThanOrEqualTo(1);
    }
}
