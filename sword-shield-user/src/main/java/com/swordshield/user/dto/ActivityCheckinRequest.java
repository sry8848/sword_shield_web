package com.swordshield.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActivityCheckinRequest {
    @NotNull(message = "activityId 不能为空")
    private Long activityId;
}

