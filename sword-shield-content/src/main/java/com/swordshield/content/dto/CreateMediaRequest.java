package com.swordshield.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增媒体资源请求 DTO。
 *
 * <p>用于记录图片/视频等媒体资源的元信息（对象存储 Key、类型、封面、可选关联文章）。
 */
@Data
public class CreateMediaRequest {
    /** 媒体资源标题（必填）。 */
    @NotBlank(message = "title 不能为空")
    private String title;

    /** 媒体资源摘要/说明（可为空）。 */
    private String summary;

    /** 资源类型（例如 image/video，可为空则由服务端校验/默认）。 */
    private String type;

    /** 对象存储（MinIO）中的对象 Key（必填）。 */
    @NotBlank(message = "objectName 不能为空")
    private String objectName;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 关联文章 ID（可为空）。 */
    private Long articleId;
}
