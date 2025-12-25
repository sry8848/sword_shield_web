package com.swordshield.interaction.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class CommentDto {
    private Long id;
    private Long userId;
    private String targetType;
    private Long targetId;
    private String content;
    private Instant createdAt;
}

