package com.example.demo;

import com.example.demo.service.ArticleService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.default_batch_fetch_size=-1",
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "spring.jpa.defer-datasource-initialization=false",
    "spring.sql.init.mode=never"
})
@org.springframework.test.context.jdbc.Sql(scripts = "/data.sql", executionPhase = org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@org.springframework.test.context.jdbc.Sql(scripts = "/clean.sql", executionPhase = org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ArticleServiceIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        statistics = sessionFactory.getStatistics();
        statistics.clear();
    }

    @Test
    void testNaiveApproach_triggersNPlusOne() {
        var articles = articleService.findAllNaive();
        
        assertThat(articles).hasSize(5);
        
        long queryCount = statistics.getPrepareStatementCount();
        // 1 query for Articles + 5 queries for Categories = 6 queries
        assertThat(queryCount).isEqualTo(6);
    }

    @Test
    void testEntityGraph_avoidsNPlusOne() {
        var articles = articleService.findAllOptimizedEntityGraph();
        
        assertThat(articles).hasSize(5);
        
        long queryCount = statistics.getPrepareStatementCount();
        // Exactly 1 query fetching Articles AND Categories
        assertThat(queryCount).isEqualTo(1);
    }

    @Test
    void testJoinFetch_avoidsNPlusOne() {
        var articles = articleService.findAllOptimizedJoinFetch();
        
        assertThat(articles).hasSize(5);
        
        long queryCount = statistics.getPrepareStatementCount();
        // Exactly 1 query fetching Articles AND Categories
        assertThat(queryCount).isEqualTo(1);
    }
}
