package com.swordshield.admin.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ActivityDto {
    private Long id;
    private String title;
    private String summary;
    private String coverUrl;
    private Instant startTime;
    private Instant endTime;
    private String location;
    private String speaker;
    private String liveUrl;
    private String replayUrl;
    private Integer maxRegistrations;
    private Integer currentRegistrations;
    private Integer heat;
    private Integer status;
    private Instant createdAt;
    private Instant updatedAt;
}
