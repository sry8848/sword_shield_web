package com.swordshield.content.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 素材 DTO。
 *
 * <p>用于素材列表/详情返回的数据结构，包含对象存储元数据与创建时间等信息。
 */
@Data
public class MaterialDto {
    /** 素材 ID。 */
    private Long id;

    /** 素材标题。 */
    private String title;

    /** 讲师/分享者（可为空）。 */
    private String speaker;

    /** 对象存储（MinIO）中的对象 Key。 */
    private String objectName;

    /** 文件 MIME 类型（可为空）。 */
    private String contentType;

    /** 文件大小（字节）。 */
    private long size;

    /** 创建时间。 */
    private Instant createdAt;
}
