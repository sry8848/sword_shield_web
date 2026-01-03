package com.swordshield.api.activity;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 活动 DTO（跨服务契约）。
 *
 * <p>用于活动服务对外（例如管理端）或服务间调用时传输活动基础信息。
 * 对应活动领域对象：{@link com.swordshield.activity.domain.Activity}。
 */
@Data
public class ActivityDto implements Serializable {
    /** 活动 ID。 */
    private Long id;

    /** 活动标题。 */
    private String title;

    /** 活动摘要/简介（可为空）。 */
    private String summary;

    /** 封面图片 URL（可为空）。 */
    private String coverUrl;

    /** 活动开始时间（可为空）。 */
    private Instant startTime;

    /** 活动结束时间（可为空）。 */
    private Instant endTime;

    /** 活动地点/地址（可为空）。 */
    private String location;

    /** 演讲嘉宾/讲师（可为空）。 */
    private String speaker;

    /** 直播链接（可为空）。 */
    private String liveUrl;

    /** 回放链接（可为空）。 */
    private String replayUrl;

    /** 最大可报名人数；为空表示不限制。 */
    private Integer maxRegistrations;

    /** 当前报名人数。 */
    private Integer currentRegistrations;

    /** 热度/人气值。 */
    private Integer heat;

    /** 状态：0-下线，1-发布。 */
    private Integer status;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;
}
