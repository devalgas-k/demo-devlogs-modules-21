package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
public class OptimizedArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private OffsetDateTime publishedAt;

    public OptimizedArticle() {}

    public OptimizedArticle(Long id, String title, OffsetDateTime publishedAt) {
        this.id = id;
        this.title = title;
        this.publishedAt = publishedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }

    public static OptimizedArticleBuilder builder() {
        return new OptimizedArticleBuilder();
    }

    public static class OptimizedArticleBuilder {
        private Long id;
        private String title;
        private OffsetDateTime publishedAt;

        public OptimizedArticleBuilder id(Long id) { this.id = id; return this; }
        public OptimizedArticleBuilder title(String title) { this.title = title; return this; }
        public OptimizedArticleBuilder publishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public OptimizedArticle build() {
            return new OptimizedArticle(id, title, publishedAt);
        }
    }
}
