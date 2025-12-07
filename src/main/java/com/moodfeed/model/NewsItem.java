package com.moodfeed.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "news_items", indexes = @Index(columnList = "link", unique = true))
@Data
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String title;

    @Column(columnDefinition = "TEXT", unique = true)
    private String link;

    private LocalDateTime publishedDate;

    private String source;

    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;

    private int score;
}
