package com.example.demo;

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
class ModeBJwtNotBeforeTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void notBeforeInFutureYields401() {
        String token = rest.getForObject("/auth/jwt?sub=alice&scope=read&nbfSecondsInFuture=3600", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = rest.exchange("/api/secure", HttpMethod.GET, entity, String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}
