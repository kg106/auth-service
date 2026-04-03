package com.example.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request object for user login")
public class LoginRequest {
    @NotBlank
    @Email
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;

    @NotBlank
    @Schema(description = "User's password", example = "StrongPassword123!")
    private String password;

    @Schema(description = "Optional device identifier", example = "device-123")
    private String deviceId; // Optional, generated if not provided
}
