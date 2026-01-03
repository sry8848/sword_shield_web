package com.swordshield.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 目标动作请求 DTO。
 *
 * <p>用于只需要目标信息的动作接口（例如点赞/取消点赞），携带 {@code targetType}/{@code targetId}。
 */
@Data
public class TargetActionRequest {
    /** 目标类型（例如 article/activity）。 */
    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    /** 目标对象 ID（例如文章 ID、活动 ID）。 */
    @NotNull(message = "targetId 不能为空")
    private Long targetId;
}
