package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 文章 DTO。
 *
 * <p>用于文章列表/详情等接口的响应数据结构，是 {@link com.swordshield.content.domain.ContentArticle} 的对外投影。
 */
@Data
public class ContentArticleDto {
    /** 文章 ID。 */
    private Long id;

    /** 文章标题。 */
    private String title;

    /** 文章摘要/简介。 */
    private String summary;

    /** 文章正文内容（详情场景返回）。 */
    private String body;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 文章分类/频道。 */
    private String category;

    /** 作者名称/署名（可为空）。 */
    private String author;

    /** 阅读量/浏览次数。 */
    private int views;

    /** 直播链接（可为空）。 */
    private String liveUrl;

    /** 回放链接（可为空）。 */
    private String replayUrl;

    /** 状态：0-草稿，1-发布。 */
    private int status;

    /** 发布时间（发布时写入；草稿状态通常为空）。 */
    private Instant publishedAt;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;
}
