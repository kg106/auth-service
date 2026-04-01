package com.example.auth_service.security;

import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final RsaKeyManager rsaKeyManager;
    private final long JWT_EXPIRATION_MS = 900000; // 15 minutes

    public JwtTokenProvider(RsaKeyManager rsaKeyManager) {
        this.rsaKeyManager = rsaKeyManager;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION_MS);

        List<String> roles = List.of(user.getRole().getName());

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId() != null ? user.getId().toString() : null)
                .claim("tenantId", user.getTenant() != null && user.getTenant().getId() != null ? user.getTenant().getId().toString() : null)
                .claim("status", user.getStatus())
                .claim("roles", roles)
                .id(UUID.randomUUID().toString()) // jti
                .issuedAt(now)
                .expiration(expiryDate)
                .header().add("kid", rsaKeyManager.getKeyId())
                .and()
                .signWith(rsaKeyManager.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public Claims validateTokenAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(rsaKeyManager.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
