package com.swordshield.activity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActivityRegisterRequest {
    @NotNull(message = "activityId 不能为空")
    private Long activityId;

    private String channel;
}

