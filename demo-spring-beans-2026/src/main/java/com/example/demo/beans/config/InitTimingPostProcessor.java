package com.example.demo.beans.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public final class InitTimingPostProcessor implements BeanPostProcessor {
    private final Map<String, Long> starts = new ConcurrentHashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        starts.put(beanName, System.nanoTime());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Long start = starts.remove(beanName);
        if (start != null) {
            long tookNanos = System.nanoTime() - start;
            if (tookNanos > 10_000_000) {
                System.out.println(beanName + " init took " + tookNanos + "ns");
            }
        }
        return bean;
    }
}
