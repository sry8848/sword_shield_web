package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 管理端文章新增/更新请求 DTO。
 *
 * <p>用于后台对文章进行创建或编辑（upsert），字段与 {@link com.swordshield.content.domain.ContentArticle} 基本对应。
 */
@Data
public class AdminArticleUpsertRequest {
    /** 文章标题。 */
    private String title;

    /** 文章摘要/简介。 */
    private String summary;

    /** 文章正文内容。 */
    private String body;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 文章分类/频道（可为空；为空时服务端可能设置默认值）。 */
    private String category;

    /** 作者名称/署名（可为空）。 */
    private String author;

    /** 状态：0-草稿，1-发布。 */
    private int status;

    /** 直播链接（可为空）。 */
    private String liveUrl;

    /** 回放链接（可为空）。 */
    private String replayUrl;

    /** 发布时间（可为空；发布时可指定，否则服务端可能使用当前时间）。 */
    private Instant publishedAt;
}
