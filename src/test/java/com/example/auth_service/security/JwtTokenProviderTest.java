package com.example.auth_service.security;

import com.example.auth_service.entity.Tenant;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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
        UserRole role = UserRole.builder().rolename("ROLE_ADMIN").build();
        Tenant tenant = Tenant.builder().id(999L).build();
        User user = User.builder()
                .id(100L)
                .email("test@example.com")
                .tenant(tenant)
                .status("ACTIVE")
                .roles(Set.of(role))
                .build();

        String token = jwtTokenProvider.generateAccessToken(user);
        assertNotNull(token);

        Claims claims = jwtTokenProvider.validateTokenAndGetClaims(token);

        assertEquals("test@example.com", claims.getSubject());
        assertEquals(100L, claims.get("userId", Long.class));
        assertEquals(999L, claims.get("tenantId", Long.class));
        assertEquals("ACTIVE", claims.get("status", String.class));
        
        List<?> roles = claims.get("roles", List.class);
        assertTrue(roles.contains("ROLE_ADMIN"));
        assertNotNull(claims.getId()); // jti should exist
    }
}
