package com.swordshield.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * 新增素材请求 DTO。
 *
 * <p>用于在内容服务侧记录一份已上传至对象存储的素材元数据（例如讲义/资料）。
 */
@Data
public class CreateMaterialRequest {
    /** 素材标题（必填）。 */
    @NotBlank(message = "title 不能为空")
    private String title;

    /** 讲师/分享者（可为空）。 */
    private String speaker;

    /** 对象存储（MinIO）中的对象 Key（必填）。 */
    @NotBlank(message = "objectName 不能为空")
    private String objectName;

    /** 文件 MIME 类型（可为空）。 */
    private String contentType;

    /** 文件大小（字节，不能为负）。 */
    @PositiveOrZero(message = "size 不能为负数")
    private Long size;
}
