package com.example.demo.repository;

import com.example.demo.domain.Article;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "categories")
    @Query("select a from Article a")
    List<Article> findAllWithCategoriesEntityGraph();

    @Query("select distinct a from Article a left join fetch a.categories")
    List<Article> findAllWithCategoriesJoinFetch();
}

