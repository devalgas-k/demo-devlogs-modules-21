package com.example.demo.beanslifecycle;

import com.example.demo.beanslifecycle.domain.ExpensiveBean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

class DestroyCallbacksTest {

    @Test
    void shouldCallPreDestroyWhenBeanWasInstantiated() {
        ExpensiveBean.resetCounters();

        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                DemoSpringBeansInjectionLifecycle2026Application.class
        ).properties(
                "spring.main.web-application-type=none"
        ).run("--demo.mode=slow");

        Assertions.assertEquals(1, ExpensiveBean.constructionCount());
        Assertions.assertEquals(0, ExpensiveBean.destroyCount());

        context.close();

        Assertions.assertEquals(1, ExpensiveBean.destroyCount());
    }

    @Test
    void shouldNotCallPreDestroyWhenBeanWasNeverInstantiated() {
        ExpensiveBean.resetCounters();

        ConfigurableApplicationContext context = new SpringApplicationBuilder(
                DemoSpringBeansInjectionLifecycle2026Application.class
        ).properties(
                "spring.main.web-application-type=none"
        ).run("--demo.mode=fast");

        Assertions.assertEquals(0, ExpensiveBean.constructionCount());
        Assertions.assertEquals(0, ExpensiveBean.destroyCount());

        context.close();

        Assertions.assertEquals(0, ExpensiveBean.destroyCount());
    }
}
