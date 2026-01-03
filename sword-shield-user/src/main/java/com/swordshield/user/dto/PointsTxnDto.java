package com.swordshield.user.dto;

import lombok.Data;

import java.time.Instant;

/**
 * 积分流水 DTO。
 *
 * <p>用于积分明细列表返回，来源于 {@link com.swordshield.user.domain.PointsTxn}。
 */
@Data
public class PointsTxnDto {
    /** 流水 ID。 */
    private Long id;

    /** 变动类型（例如 daily_checkin / activity_checkin）。 */
    private String type;

    /** 变动积分值。 */
    private Integer points;

    /** 备注说明（可为空）。 */
    private String note;

    /** 发生时间。 */
    private Instant createdAt;
}
