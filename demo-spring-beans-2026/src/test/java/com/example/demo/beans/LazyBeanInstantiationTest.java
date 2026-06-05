package com.example.demo.beans;

import com.example.demo.beans.domain.ExpensiveBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DemoSpringBeans2026Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("fast")
class LazyBeanInstantiationTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ObjectProvider<ExpensiveBean> expensiveBeanProvider;

    @Test
    void expensiveBeanIsNotInstantiatedUntilRequested() {
        assertThat(beanFactory.containsSingleton("expensiveBean")).isFalse();
        ExpensiveBean created = expensiveBeanProvider.getObject();
        assertThat(created).isNotNull();
        assertThat(beanFactory.containsSingleton("expensiveBean")).isTrue();
    }
}
