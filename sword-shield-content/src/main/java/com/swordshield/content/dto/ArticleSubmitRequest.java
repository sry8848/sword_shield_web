package com.swordshield.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户投稿提交请求 DTO。
 *
 * <p>用于用户侧提交文章内容（通常为 UGC 场景），后续是否发布由管理员审核/设置。
 */
@Data
public class ArticleSubmitRequest {
    /** 文章标题（必填）。 */
    @NotBlank(message = "title 不能为空")
    private String title;

    /** 文章摘要/简介（必填）。 */
    @NotBlank(message = "summary 不能为空")
    private String summary;

    /** 文章正文内容（必填）。 */
    @NotBlank(message = "body 不能为空")
    private String body;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 文章分类/频道（可选）。 */
    private String category;
}
