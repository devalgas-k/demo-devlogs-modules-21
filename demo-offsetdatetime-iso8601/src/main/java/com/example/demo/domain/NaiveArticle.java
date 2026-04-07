package com.example.demo.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
public class NaiveArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime publishedAtLocal;

    private ZonedDateTime publishedAtZoned;

    public NaiveArticle() {}

    public NaiveArticle(Long id, String title, LocalDateTime publishedAtLocal, ZonedDateTime publishedAtZoned) {
        this.id = id;
        this.title = title;
        this.publishedAtLocal = publishedAtLocal;
        this.publishedAtZoned = publishedAtZoned;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getPublishedAtLocal() { return publishedAtLocal; }
    public void setPublishedAtLocal(LocalDateTime publishedAtLocal) { this.publishedAtLocal = publishedAtLocal; }
    public ZonedDateTime getPublishedAtZoned() { return publishedAtZoned; }
    public void setPublishedAtZoned(ZonedDateTime publishedAtZoned) { this.publishedAtZoned = publishedAtZoned; }

    public static NaiveArticleBuilder builder() {
        return new NaiveArticleBuilder();
    }

    public static class NaiveArticleBuilder {
        private Long id;
        private String title;
        private LocalDateTime publishedAtLocal;
        private ZonedDateTime publishedAtZoned;

        public NaiveArticleBuilder id(Long id) { this.id = id; return this; }
        public NaiveArticleBuilder title(String title) { this.title = title; return this; }
        public NaiveArticleBuilder publishedAtLocal(LocalDateTime publishedAtLocal) { this.publishedAtLocal = publishedAtLocal; return this; }
        public NaiveArticleBuilder publishedAtZoned(ZonedDateTime publishedAtZoned) { this.publishedAtZoned = publishedAtZoned; return this; }
        public NaiveArticle build() {
            return new NaiveArticle(id, title, publishedAtLocal, publishedAtZoned);
        }
    }
}
