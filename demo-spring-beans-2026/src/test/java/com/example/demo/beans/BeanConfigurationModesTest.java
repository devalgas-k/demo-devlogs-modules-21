package com.example.demo.beans;

import com.example.demo.beans.config.BrokenFastBeansConfig;
import com.example.demo.beans.config.FastBeansConfig;
import com.example.demo.beans.config.SlowBeansConfig;
import com.example.demo.beans.domain.WidgetRepository;
import com.example.demo.beans.domain.WidgetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DemoSpringBeans2026Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("slow")
class SlowModeTest {

    @Autowired
    private SlowBeansConfig slowBeansConfig;

    @Autowired
    private WidgetRepository widgetRepository;

    @Autowired
    private WidgetService widgetService;

    @Test
    void configIsEnhancedAndSingletonSemanticsStillHold() {
        assertThat(slowBeansConfig.getClass().getName()).contains("$$SpringCGLIB$$");
        assertThat(widgetService.widgetRepository()).isSameAs(widgetRepository);
    }
}

@SpringBootTest(classes = DemoSpringBeans2026Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("fast")
class FastModeTest {

    @Autowired
    private FastBeansConfig fastBeansConfig;

    @Autowired
    private WidgetRepository widgetRepository;

    @Autowired
    private WidgetService widgetService;

    @Test
    void configIsNotEnhancedAndWiringIsStillCorrect() {
        assertThat(fastBeansConfig.getClass().getName()).doesNotContain("$$SpringCGLIB$$");
        assertThat(widgetService.widgetRepository()).isSameAs(widgetRepository);
    }
}

@SpringBootTest(classes = DemoSpringBeans2026Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("broken")
class BrokenModeTest {

    @Autowired
    private BrokenFastBeansConfig brokenFastBeansConfig;

    @Autowired
    private WidgetRepository widgetRepository;

    @Autowired
    private WidgetService widgetService;

    @Test
    void liteModePlusInterBeanCallCreatesTwoInstances() {
        assertThat(brokenFastBeansConfig.getClass().getName()).doesNotContain("$$SpringCGLIB$$");
        assertThat(widgetService.widgetRepository()).isNotSameAs(widgetRepository);
        assertThat(widgetService.widgetRepository().instanceId()).isNotEqualTo(widgetRepository.instanceId());
    }
}
