package com.swordshield.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 活动签到请求 DTO。
 *
 * <p>用于用户对指定活动进行签到，携带 {@code activityId}。
 */
@Data
public class ActivityCheckinRequest {
    /** 要签到的活动 ID。 */
    @NotNull(message = "activityId 不能为空")
    private Long activityId;
}
