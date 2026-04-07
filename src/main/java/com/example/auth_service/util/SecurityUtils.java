package com.example.auth_service.util;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    public UUID getTenantIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getDetails() == null) {
            throw new RuntimeException("Authentication details not found");
        }

        Object details = authentication.getDetails();
        if (!(details instanceof Claims)) {
            throw new RuntimeException("Invalid authentication details type");
        }

        Claims claims = (Claims) details;
        String tenantIdStr = claims.get("tenantId", String.class);
        if (tenantIdStr == null) {
            throw new RuntimeException("Tenant ID not found in token");
        }

        return UUID.fromString(tenantIdStr);
    }
}
