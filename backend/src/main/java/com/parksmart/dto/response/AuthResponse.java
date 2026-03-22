package com.parksmart.dto.response;

import com.parksmart.entity.User.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data @Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private UUID userId;
    private String name;
    private String email;
    private Role role;
}
