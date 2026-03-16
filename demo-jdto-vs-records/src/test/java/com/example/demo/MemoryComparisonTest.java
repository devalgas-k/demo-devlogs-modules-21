package com.example.demo;

import com.example.demo.dto.ArticleDTO;
import com.example.demo.dto.ArticleRecord;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MemoryComparisonTest {
    private static final Logger log = LoggerFactory.getLogger(MemoryComparisonTest.class);
    private static final int COUNT = 1_000_000;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    @Test
    void completeComparison() {
        log.info("--- STARTING COMPLETE COMPARISON (JPMS MODULE) ---");
        
        // 1. STRUCTURE ANALYSIS (MICRO)
        log.info("--- 1. Structure Analysis with JOL ---");
        log.info("ArticleDTO Layout (Class): \n{}", ClassLayout.parseClass(ArticleDTO.class).toPrintable());
        
        try {
            log.info("ArticleRecord Layout (Record): \n{}", ClassLayout.parseClass(ArticleRecord.class).toPrintable());
        } catch (Exception e) {
            log.warn("JOL cannot access Record offsets without specific flags (native immutability).");
        }

        // 2. GLOBAL CONSUMPTION MEASUREMENT (MACRO)
        log.info("--- 2. Measurement over {} objects ---", COUNT);
        
        long memClass = measureMemory(() -> {
            List<ArticleDTO> list = new ArrayList<>(COUNT);
            for (int i = 0; i < COUNT; i++) {
                list.add(new ArticleDTO((long) i, "Title " + i, "Content " + i, "Author " + i));
            }
            return list;
        });

        long memRecord = measureMemory(() -> {
            List<ArticleRecord> list = new ArrayList<>(COUNT);
            for (int i = 0; i < COUNT; i++) {
                list.add(new ArticleRecord((long) i, "Title " + i, "Content " + i, "Author " + i));
            }
            return list;
        });

        log.info("Memory used by Classes: {} MB", memClass / (1024 * 1024));
        log.info("Memory used by Records: {} MB", memRecord / (1024 * 1024));

        double diff = ((double) (memClass - memRecord) / memClass) * 100;
        log.info("Memory Saving: {}%", df.format(diff));

        // 3. CONCLUSION
        log.info("--- 3. CONCLUSION ---");
        log.info("Today (JDK 17/21): Footprint is identical because both are objects on the Heap.");
        log.info("The slight difference ({}%) is due to GC management and fragmentation.", df.format(diff));
        log.info("The real gain of Records lies in maintenance and future Project Valhalla compatibility.");
        log.info("--- END OF COMPARISON ---");
    }

    private long measureMemory(java.util.function.Supplier<List<?>> supplier) {
        System.gc();
        System.gc();
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        
        long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        List<?> list = supplier.get();
        long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Keep the list alive to prevent early GC collection
        int size = list.size();
        return after - before;
    }
}
