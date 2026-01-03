package com.swordshield.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分享请求 DTO。
 *
 * <p>用于记录一次分享行为：目标类型、目标 ID，以及可选的分享渠道。
 */
@Data
public class ShareRequest {
    /** 目标类型（例如 article/activity）。 */
    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    /** 目标对象 ID（例如文章 ID、活动 ID）。 */
    @NotNull(message = "targetId 不能为空")
    private Long targetId;

    /** 分享渠道（例如 wechat/weibo/link）；为空时服务端可能默认 {@code link}。 */
    private String channel;
}
