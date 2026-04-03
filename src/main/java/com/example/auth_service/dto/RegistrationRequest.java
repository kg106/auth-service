package com.example.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request object for user registration")
public class RegistrationRequest {
    @NotBlank
    @Schema(description = "Unique identifier for the tenant", example = "tenant-123")
    private String tenantIdStr;

    @NotBlank
    @Email
    @Schema(description = "User's email address", example = "user@example.com")
    private String email;

    @NotBlank
    @Schema(description = "User's password", example = "StrongPassword123!")
    private String password;

    @NotBlank
    @Schema(description = "User's first name", example = "John")
    private String firstname;

    @NotBlank
    @Schema(description = "User's last name", example = "Doe")
    private String lastname;

    @NotBlank
    @Schema(description = "User's mobile number", example = "+1234567890")
    private String mobileNumber;
}
