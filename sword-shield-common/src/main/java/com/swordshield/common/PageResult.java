package com.swordshield.common;

import java.util.List;

/**
 * 分页结果类
 * 用于封装分页查询的结果数据，包含当前页数据列表、总记录数、当前页码和每页大小
 * @param <T> 分页数据的泛型类型
 */
public class PageResult<T> {
    private List<T> items;  // 当前页的数据列表
    private long total;     // 总记录数
    private int page;       // 当前页码
    private int size;       // 每页大小

    /**
     * 创建分页结果对象的静态工厂方法
     * @param items 当前页的数据列表
     * @param total 总记录数
     * @param page 当前页码
     * @param size 每页大小
     * @param <T> 分页数据的泛型类型
     * @return PageResult<T> 分页结果对象
     */
    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.items = items;
        r.total = total;
        r.page = page;
        r.size = size;
        return r;
    }

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}

