package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class MediaAssetDto {
    private Long id;
    private String title;
    private String summary;
    private String type;
    private String objectName;
    private String coverUrl;
    private Long articleId;
    private Instant createdAt;
}

