package com.swordshield.content.dto;

import lombok.Data;

/**
 * 展商 DTO。
 *
 * <p>用于展商列表/详情接口返回的数据结构，来源于 {@link com.swordshield.content.domain.Exhibitor}。
 */
@Data
public class ExhibitorDto {
    /** 展商 ID。 */
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
}
