package com.swordshield.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareRequest {
    @NotBlank(message = "targetType 不能为空")
    private String targetType;

    @NotNull(message = "targetId 不能为空")
    private Long targetId;

    private String channel;
}

