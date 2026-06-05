package com.example.demo.beans.web;

import com.example.demo.beans.config.BrokenFastBeansConfig;
import com.example.demo.beans.config.FastBeansConfig;
import com.example.demo.beans.config.SlowBeansConfig;
import com.example.demo.beans.domain.ExpensiveBean;
import com.example.demo.beans.domain.WidgetRepository;
import com.example.demo.beans.domain.WidgetService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BeanInfoController {

    private final WidgetService widgetService;
    private final WidgetRepository widgetRepository;
    private final ConfigurableListableBeanFactory beanFactory;
    private final ObjectProvider<ExpensiveBean> expensiveBeanProvider;
    private final ObjectProvider<SlowBeansConfig> slowBeansConfig;
    private final ObjectProvider<FastBeansConfig> fastBeansConfig;
    private final ObjectProvider<BrokenFastBeansConfig> brokenFastBeansConfig;

    public BeanInfoController(
            WidgetService widgetService,
            WidgetRepository widgetRepository,
            ConfigurableListableBeanFactory beanFactory,
            ObjectProvider<ExpensiveBean> expensiveBeanProvider,
            ObjectProvider<SlowBeansConfig> slowBeansConfig,
            ObjectProvider<FastBeansConfig> fastBeansConfig,
            ObjectProvider<BrokenFastBeansConfig> brokenFastBeansConfig
    ) {
        this.widgetService = widgetService;
        this.widgetRepository = widgetRepository;
        this.beanFactory = beanFactory;
        this.expensiveBeanProvider = expensiveBeanProvider;
        this.slowBeansConfig = slowBeansConfig;
        this.fastBeansConfig = fastBeansConfig;
        this.brokenFastBeansConfig = brokenFastBeansConfig;
    }

    @GetMapping("/api/beans")
    public Map<String, Object> beans() {
        Map<String, Object> payload = new LinkedHashMap<>();
        Object config = slowBeansConfig.getIfAvailable();
        String profile = "slow";
        if (config == null) {
            config = fastBeansConfig.getIfAvailable();
            profile = "fast";
        }
        if (config == null) {
            config = brokenFastBeansConfig.getIfAvailable();
            profile = "broken";
        }

        payload.put("activeProfile", config == null ? "unknown" : profile);

        payload.put("configClass", config == null ? null : config.getClass().getName());
        payload.put("configEnhancedByCglib", config != null && config.getClass().getName().contains("$$SpringCGLIB$$"));
        payload.put("widgetRepository.instanceId", widgetRepository.instanceId());
        payload.put("widgetService.widgetRepository.instanceId", widgetService.widgetRepository().instanceId());
        payload.put("repositorySameInstance", widgetRepository == widgetService.widgetRepository());
        payload.put("expensiveBean.beanName", "expensiveBean");
        payload.put("expensiveBean.instantiated", beanFactory.containsSingleton("expensiveBean"));
        return payload;
    }

    @GetMapping("/api/beans/expensive")
    public Map<String, Object> expensiveBean(@RequestParam(name = "create", defaultValue = "false") boolean create) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("beanName", "expensiveBean");
        payload.put("instantiatedBefore", beanFactory.containsSingleton("expensiveBean"));
        if (create) {
            ExpensiveBean bean = expensiveBeanProvider.getObject();
            payload.put("createdAt", bean.createdAt());
            payload.put("instanceId", bean.instanceId());
        }
        payload.put("instantiatedAfter", beanFactory.containsSingleton("expensiveBean"));
        return payload;
    }
}
