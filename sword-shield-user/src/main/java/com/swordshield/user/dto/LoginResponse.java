package com.swordshield.user.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String userId;
}

