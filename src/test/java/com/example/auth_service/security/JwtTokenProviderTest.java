package com.example.auth_service.security;

import com.example.auth_service.entity.Tenant;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private RsaKeyManager rsaKeyManager;

    @BeforeEach
    void setUp() {
        rsaKeyManager = new RsaKeyManager();
        jwtTokenProvider = new JwtTokenProvider(rsaKeyManager);
    }

    @Test
    void testGenerateAndValidateToken() {
        UserRole role = UserRole.builder().name("ROLE_ADMIN").build();
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        
        Tenant tenant = Tenant.builder().id(tenantId).build();
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .tenant(tenant)
                .status("ACTIVE")
                .role(role)
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);
        assertNotNull(token);

        Claims claims = jwtTokenProvider.validateTokenAndGetClaims(token);

        assertEquals("test@example.com", claims.getSubject());
        assertEquals(userId.toString(), claims.get("userId", String.class));
        assertEquals(tenantId.toString(), claims.get("tenantId", String.class));
        assertEquals("ACTIVE", claims.get("status", String.class));
        
        List<?> roles = claims.get("roles", List.class);
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertNotNull(claims.getId()); // jti should exist
    }
}
