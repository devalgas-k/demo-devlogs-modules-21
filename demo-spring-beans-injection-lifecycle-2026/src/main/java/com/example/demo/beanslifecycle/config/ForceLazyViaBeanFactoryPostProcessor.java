package com.example.demo.beanslifecycle.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "demo", name = "forceLazyViaBfpp", havingValue = "true")
public class ForceLazyViaBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        if (beanFactory.containsBeanDefinition("expensiveBean")) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition("expensiveBean");
            beanDefinition.setLazyInit(true);
        }
    }
}

