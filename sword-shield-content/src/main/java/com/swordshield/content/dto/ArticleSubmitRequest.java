package com.swordshield.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArticleSubmitRequest {
    @NotBlank(message = "title 不能为空")
    private String title;

    @NotBlank(message = "summary 不能为空")
    private String summary;

    @NotBlank(message = "body 不能为空")
    private String body;

    private String coverUrl;

    private String category;
}

