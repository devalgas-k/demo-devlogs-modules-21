package com.example.demo.beanslifecycle;

import com.example.demo.beanslifecycle.domain.ExpensiveBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "demo.mode=slow"
)
@DirtiesContext
class SlowModeEagerInstantiationTest {

    @TestConfiguration
    static class ResetCountersConfig {
        @Bean
        static BeanFactoryPostProcessor resetExpensiveBeanCounters() {
            return beanFactory -> ExpensiveBean.resetCounters();
        }
    }

    @Test
    void shouldInstantiateExpensiveBeanAtStartup() {
        Assertions.assertEquals(1, ExpensiveBean.constructionCount());
    }
}

