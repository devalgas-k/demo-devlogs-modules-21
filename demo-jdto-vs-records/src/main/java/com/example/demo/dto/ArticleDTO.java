package com.example.demo.dto;

import java.util.Objects;

/**
 * Standard POJO representing a DTO.
 */
public class ArticleDTO {
    private final Long id;
    private final String title;
    private final String content;
    private final String author;

    public ArticleDTO(Long id, String title, String content, String author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArticleDTO that = (ArticleDTO) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(title, that.title) &&
               Objects.equals(content, that.content) &&
               Objects.equals(author, that.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, author);
    }

    @Override
    public String toString() {
        return "ArticleDTO{" +
               "id=" + id +
               ", title='" + title + '\'' +
               ", content='" + content + '\'' +
               ", author='" + author + '\'' +
               '}';
    }
}
