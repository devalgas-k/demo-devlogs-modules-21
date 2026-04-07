package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import com.example.demo.domain.OptimizedArticle;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DateTimeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldHandleOffsetDateTime() {
        ResponseEntity<OptimizedArticle> response = restTemplate.getForEntity("/api/articles/optimized", OptimizedArticle.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody().getPublishedAt()).isNotNull();
    }
}
