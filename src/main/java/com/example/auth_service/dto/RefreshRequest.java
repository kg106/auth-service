package com.example.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class RefreshRequest {
    @NotBlank
    private String refreshToken;

    @NotBlank
    private String deviceId;

    @NotNull
    private UUID userId;
}
