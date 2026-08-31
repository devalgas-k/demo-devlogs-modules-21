package com.example.demo.beanslifecycle;

import com.example.demo.beanslifecycle.domain.ExpensiveBean;
import com.example.demo.beanslifecycle.domain.ExpensiveUseCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "demo.mode=slow",
                "demo.forceLazyViaBfpp=true"
        }
)
@DirtiesContext
class SlowModeForcedLazyViaBfppTest {

    @TestConfiguration
    static class ResetCountersConfig {
        @Bean
        static BeanFactoryPostProcessor resetExpensiveBeanCounters() {
            return beanFactory -> ExpensiveBean.resetCounters();
        }
    }

    @Autowired
    private ExpensiveUseCase useCase;

    @Test
    void shouldNotInstantiateExpensiveBeanAtStartupWhenForcedLazyByBfpp() {
        Assertions.assertEquals(0, ExpensiveBean.constructionCount());
        useCase.run(42);
        Assertions.assertEquals(1, ExpensiveBean.constructionCount());
    }
}

