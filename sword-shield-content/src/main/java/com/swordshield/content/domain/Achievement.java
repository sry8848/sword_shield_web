package com.swordshield.content.domain;

import jakarta.persistence.*;

/**
 * 成果展示条目（Domain Entity）。
 *
 * <p>用于「成果/案例」模块的展示数据，通常包含标题、描述及关联的文件链接（由文件服务提供）。
 */
@Entity
@Table(name = "achievement")
public class Achievement {
    /** 成果主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 成果标题。 */
    private String title;

    /** 成果描述/简介。 */
    private String description;

    /** 关联文件链接（通常由文件服务提供的可访问 URL）。 */
    private String fileUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}
