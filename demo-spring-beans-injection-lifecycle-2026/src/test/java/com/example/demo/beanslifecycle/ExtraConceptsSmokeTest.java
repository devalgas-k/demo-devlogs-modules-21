package com.example.demo.beanslifecycle;

import com.example.demo.beanslifecycle.domain.BadPrototypeConsumer;
import com.example.demo.beanslifecycle.domain.FixedClockReportService;
import com.example.demo.beanslifecycle.domain.GoodPrototypeConsumer;
import com.example.demo.beanslifecycle.domain.ImportedIdService;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "demo.mode=fast"
)
class ExtraConceptsSmokeTest {

    @Autowired
    private FixedClockReportService fixedClockReportService;

    @Autowired
    private ImportedIdService importedIdService;

    @Autowired
    private GoodPrototypeConsumer goodPrototypeConsumer;

    @Autowired
    private BadPrototypeConsumer badPrototypeConsumer;

    @Test
    void shouldUseQualifierForFixedClock() {
        Assertions.assertEquals(Instant.parse("2026-01-01T00:00:00Z"), fixedClockReportService.now());
    }

    @Test
    void shouldProvideImportedBeanViaImport() {
        Assertions.assertTrue(importedIdService.newId().startsWith("imported-"));
    }

    @Test
    void shouldShowPrototypePitfall() {
        Assertions.assertNotEquals(goodPrototypeConsumer.newWidgetId(), goodPrototypeConsumer.newWidgetId());
        Assertions.assertEquals(badPrototypeConsumer.widgetId(), badPrototypeConsumer.widgetId());
    }
}

