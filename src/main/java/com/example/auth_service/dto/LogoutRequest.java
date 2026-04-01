package com.example.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank
    private String accessTokenJwtId; // The JTI to blacklist

    @NotBlank
    private String deviceId;

    @NotNull
    private Long userId;
}
