package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ContentArticleDto {
    private Long id;
    private String title;
    private String summary;
    private String body;
    private String coverUrl;
    private String category;
    private String author;
    private int views;
    private String liveUrl;
    private String replayUrl;
    private int status;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
}

