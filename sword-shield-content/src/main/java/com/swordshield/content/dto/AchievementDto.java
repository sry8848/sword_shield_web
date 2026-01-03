package com.swordshield.content.dto;

import lombok.Data;

/**
 * 成果展示 DTO。
 *
 * <p>用于对外接口返回「成果」的基本信息（标题、描述、文件链接）。
 */
@Data
public class AchievementDto {
    /** 成果 ID。 */
    private Long id;

    /** 成果标题。 */
    private String title;

    /** 成果描述/简介。 */
    private String description;

    /** 关联文件链接（通常为文件服务提供的可访问 URL）。 */
    private String fileUrl;
}
