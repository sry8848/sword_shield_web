package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AdminArticleUpsertRequest {
    private String title;
    private String summary;
    private String body;
    private String coverUrl;
    private String category;
    private String author;
    private int status;
    private String liveUrl;
    private String replayUrl;
    private Instant publishedAt;
}

