package com.swordshield.activity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 活动报名请求 DTO。
 *
 * <p>对应用户侧「报名活动」接口的请求体：指定要报名的 {@code activityId}，可选 {@code channel} 用于记录来源渠道。
 */
@Data
public class ActivityRegisterRequest {
    /** 要报名的活动 ID。 */
    @NotNull(message = "activityId 不能为空")
    private Long activityId;

    /** 报名渠道（如 web/app/onsite）；为空时服务端通常默认 {@code web}。 */
    private String channel;
}
