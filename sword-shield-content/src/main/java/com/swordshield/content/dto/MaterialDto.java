package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class MaterialDto {
    private Long id;
    private String title;
    private String speaker;
    private String objectName;
    private String contentType;
    private long size;
    private Instant createdAt;
}

