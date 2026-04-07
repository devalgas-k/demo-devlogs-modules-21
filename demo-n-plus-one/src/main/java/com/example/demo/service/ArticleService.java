package com.example.demo.service;

import com.example.demo.domain.Article;
import com.example.demo.dto.ArticleSummaryDTO;
import com.example.demo.repository.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleService {
    private final ArticleRepository repository;

    public ArticleService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ArticleSummaryDTO> findAllNaive() {
        List<Article> articles = repository.findAll();
        return articles.stream()
                .map(a -> new ArticleSummaryDTO(
                        a.getId(),
                        a.getTitle(),
                        a.getCategories().stream().map(c -> c.getName()).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArticleSummaryDTO> findAllOptimizedEntityGraph() {
        List<Article> articles = repository.findAllWithCategoriesEntityGraph();
        return articles.stream()
                .map(a -> new ArticleSummaryDTO(
                        a.getId(),
                        a.getTitle(),
                        a.getCategories().stream().map(c -> c.getName()).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArticleSummaryDTO> findAllOptimizedJoinFetch() {
        List<Article> articles = repository.findAllWithCategoriesJoinFetch();
        return articles.stream()
                .map(a -> new ArticleSummaryDTO(
                        a.getId(),
                        a.getTitle(),
                        a.getCategories().stream().map(c -> c.getName()).collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}

