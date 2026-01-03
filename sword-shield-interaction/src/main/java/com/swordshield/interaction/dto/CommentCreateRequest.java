package com.swordshield.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 评论创建请求 DTO。
 *
 * <p>用于提交一条评论：指定目标类型与目标 ID，并提供评论内容。
 */
@Data
public class CommentCreateRequest {
    /** 目标类型（例如 article/activity）。 */
    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    /** 目标对象 ID（例如文章 ID、活动 ID）。 */
    @NotNull(message = "targetId 不能为空")
    private Long targetId;

    /** 评论内容（必填）。 */
    @NotBlank(message = "content 不能为空")
    private String content;
}
