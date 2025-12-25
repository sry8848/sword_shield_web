package com.swordshield.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMediaRequest {
    @NotBlank(message = "title 不能为空")
    private String title;

    private String summary;

    private String type;

    @NotBlank(message = "objectName 不能为空")
    private String objectName;

    private String coverUrl;

    private Long articleId;
}

