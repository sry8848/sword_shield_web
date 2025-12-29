package com.swordshield.content.repository;

import com.swordshield.content.domain.ContentArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentArticleRepository extends JpaRepository<ContentArticle, Long> {
    Page<ContentArticle> findByCategory(String category, Pageable pageable);

    Page<ContentArticle> findByStatus(int status, Pageable pageable);

    Page<ContentArticle> findByStatusAndCategory(int status, String category, Pageable pageable);

    Page<ContentArticle> findByCategoryNot(String category, Pageable pageable);

    Page<ContentArticle> findByStatusAndCategoryNot(int status, String category, Pageable pageable);
    
    @Query("SELECT a FROM ContentArticle a WHERE a.status = 1 AND a.publishedAt IS NOT NULL")
    Page<ContentArticle> findPublishedArticles(Pageable pageable);

    @Query("SELECT a FROM ContentArticle a WHERE a.status = 1 AND a.publishedAt IS NOT NULL AND (a.category IS NULL OR a.category <> :excludeCategory)")
    Page<ContentArticle> findPublishedArticlesExcludeCategory(@Param("excludeCategory") String excludeCategory, Pageable pageable);

    @Query("SELECT a FROM ContentArticle a WHERE a.status = 1 AND a.publishedAt IS NOT NULL AND a.category = :category")
    Page<ContentArticle> findPublishedArticlesByCategory(String category, Pageable pageable);
}

