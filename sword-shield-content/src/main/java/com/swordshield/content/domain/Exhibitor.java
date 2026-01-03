package com.swordshield.content.domain;

import jakarta.persistence.*;

/**
 * 展商信息（Domain Entity）。
 *
 * <p>用于展商列表/详情展示，包含名称、品牌、介绍、官网与媒体链接等字段。
 */
@Entity
@Table(name = "exhibitor")
public class Exhibitor {
    /** 展商主键 ID。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 展商名称。 */
    private String name;

    /** 展商 Logo 图片 URL（可为空）。 */
    private String logoUrl;

    /** 展商介绍（可为空）。 */
    private String introduction;

    /** 官网链接（可为空）。 */
    private String website;

    /** 品牌/厂牌（可为空）。 */
    private String brand;

    /** 宣传视频链接（可为空）。 */
    private String videoUrl;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
}
