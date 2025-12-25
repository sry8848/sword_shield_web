package com.swordshield.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class CreateMaterialRequest {
    @NotBlank(message = "title 不能为空")
    private String title;

    private String speaker;

    @NotBlank(message = "objectName 不能为空")
    private String objectName;

    private String contentType;

    @PositiveOrZero(message = "size 不能为负数")
    private Long size;
}

