package com.swordshield.interaction.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 评论 DTO。
 *
 * <p>用于评论列表/详情接口的返回数据结构，来源于 {@link com.swordshield.interaction.domain.Comment}。
 */
@Data
public class CommentDto {
    /** 评论 ID。 */
    private Long id;

    /** 评论用户 ID。 */
    private Long userId;

    /** 目标类型（例如 article/activity）。 */
    private String targetType;

    /** 目标对象 ID（例如文章 ID、活动 ID）。 */
    private Long targetId;

    /** 评论内容。 */
    private String content;

    /** 评论创建时间。 */
    private Instant createdAt;
}
