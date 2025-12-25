package com.swordshield.user.dto;

import lombok.Data;

@Data
public class PointsSummaryDto {
    private Integer total;
    private Integer available;
    private Integer used;
}

