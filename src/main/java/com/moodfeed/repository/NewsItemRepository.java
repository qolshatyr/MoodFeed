package com.moodfeed.repository;

import com.moodfeed.model.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, Long>, JpaSpecificationExecutor<NewsItem> {

    boolean existsByLink(String link);

    List<NewsItem> findByPublishedDateAfter(LocalDateTime date);
}