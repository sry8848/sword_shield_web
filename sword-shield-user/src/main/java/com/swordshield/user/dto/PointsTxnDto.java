package com.swordshield.user.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PointsTxnDto {
    private Long id;
    private String type;
    private Integer points;
    private String note;
    private Instant createdAt;
}

