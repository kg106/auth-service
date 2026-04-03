package com.example.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
@Schema(description = "Request object for refreshing access token")
public class RefreshRequest {
    @NotBlank
    @Schema(description = "UUID based refresh token issued during login", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;

    @NotBlank
    @Schema(description = "Unique identifier for the device", example = "device-12345")
    private String deviceId;

    @NotNull
    @Schema(description = "UUID of the user", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;
}
