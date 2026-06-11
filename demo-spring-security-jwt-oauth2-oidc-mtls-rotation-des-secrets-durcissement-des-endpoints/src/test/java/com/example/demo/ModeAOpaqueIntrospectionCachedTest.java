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

@ActiveProfiles("mode-a-cached")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ModeAOpaqueIntrospectionCachedTest {

    @Autowired
    TestRestTemplate rest;

    @Autowired
    SecurityStats stats;

    @Test
    void opaqueTokensAreCachedAndReduceIntrospectionCalls() {
        String token = rest.getForObject("/auth/opaque?sub=alice&scope=read", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = rest.exchange("/api/secure", HttpMethod.GET, entity, String.class);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        SecurityStats.Snapshot snapshot = stats.snapshot();
        assertThat(snapshot.opaqueIntrospections()).isEqualTo(1);
    }
}
