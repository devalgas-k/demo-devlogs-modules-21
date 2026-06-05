package com.example.demo.beans;

import com.example.demo.beans.domain.AuditService;
import com.example.demo.beans.domain.ClockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("fast")
class AuditServiceTest {

    @Autowired
    private ApplicationContext ctx;

    @Test
    void shouldHaveBeans() {
        assertThat(ctx.containsBean("auditService")).isTrue();
        assertThat(ctx.containsBean("clockProvider")).isTrue();
    }

    @Test
    void shouldInjectCorrectDependencies() {
        AuditService auditService = ctx.getBean(AuditService.class);
        ClockProvider clockProvider = ctx.getBean(ClockProvider.class);
        
        assertThat(auditService).isNotNull();
        assertThat(clockProvider).isNotNull();
    }
}
