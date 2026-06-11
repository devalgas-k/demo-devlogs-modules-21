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
class ModeBJwtNoKidTest {

    @Autowired
    TestRestTemplate rest;

    @Test
    void jwtWithoutKidStillValidatesAndSurvivesRotation() {
        String token = rest.getForObject("/auth/jwt?sub=alice&scope=read&noKid=true", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> before = rest.exchange("/api/secure", HttpMethod.GET, entity, String.class);
        assertThat(before.getStatusCode().value()).isEqualTo(200);

        rest.postForEntity("/auth/rotate", null, String.class);

        ResponseEntity<String> after = rest.exchange("/api/secure", HttpMethod.GET, entity, String.class);
        assertThat(after.getStatusCode().value()).isEqualTo(200);
    }
}
