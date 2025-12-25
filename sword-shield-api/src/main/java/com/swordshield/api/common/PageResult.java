package com.swordshield.api.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private List<T> items;
    private long total;
    private int page;
    private int size;
}

