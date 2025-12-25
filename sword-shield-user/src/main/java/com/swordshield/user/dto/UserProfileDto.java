package com.swordshield.user.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class UserProfileDto {
    private Long id;
    private String username;
    private String nickname;
    private String gender;
    private String phone;
    private String email;
    private String avatarUrl;
    private Integer status;
    private Instant createdAt;
    private Instant updatedAt;
}

