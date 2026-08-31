package com.example.demo.beanslifecycle.config;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class InitTimingBeanPostProcessor implements BeanPostProcessor {

    private final Map<String, Long> startNsByBeanName = new ConcurrentHashMap<>();
    private final Map<String, Long> initDurationNsByBeanName = new ConcurrentHashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        startNsByBeanName.put(beanName, System.nanoTime());
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Long startNs = startNsByBeanName.remove(beanName);
        if (startNs != null) {
            initDurationNsByBeanName.put(beanName, System.nanoTime() - startNs);
        }
        return bean;
    }

    public List<BeanInitTiming> top(int top) {
        return initDurationNsByBeanName.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(0, top))
                .map(entry -> new BeanInitTiming(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record BeanInitTiming(String beanName, long initDurationNs) {
    }
}
