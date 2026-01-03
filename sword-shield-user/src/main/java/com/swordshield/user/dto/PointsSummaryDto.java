package com.swordshield.user.dto;

import lombok.Data;

/**
 * 积分汇总 DTO。
 *
 * <p>用于返回用户当前积分概览（总积分/可用/已用），来源于 {@link com.swordshield.user.domain.UserPoints}。
 */
@Data
public class PointsSummaryDto {
    /** 累计获得的总积分。 */
    private Integer total;

    /** 当前可用积分。 */
    private Integer available;

    /** 已使用/已消耗的积分。 */
    private Integer used;
}
