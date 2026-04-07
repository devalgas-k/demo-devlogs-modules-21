package com.example.demo.web;

import com.example.demo.domain.NaiveArticle;
import com.example.demo.domain.OptimizedArticle;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/articles")
public class DateTimeController {

    @GetMapping("/naive")
    public NaiveArticle getNaive() {
        return NaiveArticle.builder()
                .id(1L)
                .title("Naive Article")
                .publishedAtLocal(LocalDateTime.now())
                .publishedAtZoned(ZonedDateTime.now())
                .build();
    }

    @GetMapping("/optimized")
    public OptimizedArticle getOptimized() {
        return OptimizedArticle.builder()
                .id(1L)
                .title("Optimized Article")
                .publishedAt(OffsetDateTime.now())
                .build();
    }

    @PostMapping("/optimized")
    public OptimizedArticle createOptimized(@RequestBody OptimizedArticle article) {
        // When receiving data, OffsetDateTime perfectly handles incoming ISO-8601 strings
        return article;
    }
}
