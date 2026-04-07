package com.example.demo.web;

import com.example.demo.dto.ArticleSummaryDTO;
import com.example.demo.service.ArticleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService service;

    public ArticleController(ArticleService service) {
        this.service = service;
    }

    @GetMapping("/naive")
    public List<ArticleSummaryDTO> getNaive() {
        return service.findAllNaive();
    }

    @GetMapping("/optimized/entity-graph")
    public List<ArticleSummaryDTO> getOptimizedEntityGraph() {
        return service.findAllOptimizedEntityGraph();
    }

    @GetMapping("/optimized/join-fetch")
    public List<ArticleSummaryDTO> getOptimizedJoinFetch() {
        return service.findAllOptimizedJoinFetch();
    }
}

