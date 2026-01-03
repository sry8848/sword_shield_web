package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 媒体资源 DTO。
 *
 * <p>用于图片/视频资源的列表/详情返回，来源于 {@link com.swordshield.content.domain.MediaAsset}。
 */
@Data
public class MediaAssetDto {
    /** 媒体资源 ID。 */
    private Long id;

    /** 资源标题。 */
    private String title;

    /** 资源摘要/说明（可为空）。 */
    private String summary;

    /** 资源类型：image | video。 */
    private String type;

    /** 对象存储（MinIO）中的对象 Key。 */
    private String objectName;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 关联文章 ID（可为空）。 */
    private Long articleId;

    /** 创建时间。 */
    private Instant createdAt;
}
