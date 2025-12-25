package com.swordshield.api.file;

import lombok.Data;

import java.time.Instant;

@Data
public class AssetFileDto {
    private Long id;
    private String objectName;
    private String bucket;
    private String contentType;
    private Long size;
    private Long ownerId;
    private Instant createdAt;
    private String originalName;
}
