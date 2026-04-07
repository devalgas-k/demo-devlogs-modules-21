package com.example.demo.dto;

import java.util.List;

public class ArticleSummaryDTO {
    private Long id;
    private String title;
    private List<String> categories;

    public ArticleSummaryDTO(Long id, String title, List<String> categories) {
        this.id = id;
        this.title = title;
        this.categories = categories;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public List<String> getCategories() { return categories; }
}

