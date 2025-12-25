package com.swordshield.user.dto;

import lombok.Data;

@Data
public class UpdateUserProfileRequest {
    private Long id;
    private String nickname;
    private String gender;
    private String phone;
    private String email;
    private String avatarUrl;
}

