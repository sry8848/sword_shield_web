package com.swordshield.api.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果 DTO（跨服务契约）。
 *
 * <p>用于 RPC 场景返回分页列表及总数等信息。
 */
@Data
public class PageResult<T> implements Serializable {
    /** 当前页数据列表。 */
    private List<T> items;

    /** 符合条件的总记录数。 */
    private long total;

    /** 当前页码（从 0 开始）。 */
    private int page;

    /** 每页大小。 */
    private int size;
}
