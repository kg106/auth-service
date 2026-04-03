package com.example.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing JWT and session details")
public class AuthResponse {
    @Schema(description = "RS256 signed access token", example = "eyJhbGciOiJSUzI1NiIs...")
    private String accessToken;

    @Schema(description = "UUID based refresh token", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;

    @Schema(description = "Unique identifier for the device", example = "dev-123-abc")
    private String deviceId;

    @Schema(description = "Unique identifier for the user", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;
}
