package com.example.auth_service.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class TokenStoreService {

    private final StringRedisTemplate redisTemplate;
    private final long REFRESH_TOKEN_TTL_DAYS = 7;
    private final long ACCESS_TOKEN_TTL_MINUTES = 15;
    private final long RESET_TOKEN_TTL_MINUTES = 15;

    public TokenStoreService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeRefreshToken(UUID userId, String deviceId, String rawRefreshToken) {
        String key = "session:" + userId + ":" + deviceId;
        String hash = hashToken(rawRefreshToken);
        @SuppressWarnings("null")
        Duration ttl = Duration.ofDays(REFRESH_TOKEN_TTL_DAYS);
        redisTemplate.opsForValue().set(key, hash, ttl);
    }

    public boolean validateAndRotateRefreshToken(UUID userId, String deviceId, String rawRefreshToken) {
        String key = "session:" + userId + ":" + deviceId;
        String storedHash = redisTemplate.opsForValue().get(key);

        if (storedHash == null) {
            return false;
        }

        String incomingHash = hashToken(rawRefreshToken);
        if (storedHash.equals(incomingHash)) {
            // Valid token -> Delete it immediately to enforce rotation
            redisTemplate.delete(key);
            return true;
        }

        // Token mismatch (potential reuse) -> Delete session to be safe
        redisTemplate.delete(key);
        return false;
    }

    public void blacklistAccessToken(String jti) {
        String key = "blacklist:" + jti;
        @SuppressWarnings("null")
        Duration ttl = Duration.ofMinutes(ACCESS_TOKEN_TTL_MINUTES);
        redisTemplate.opsForValue().set(key, "revoked", ttl);
    }

    public boolean isAccessTokenBlacklisted(String jti) {
        String key = "blacklist:" + jti;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    public void deleteSession(UUID userId, String deviceId) {
        String key = "session:" + userId + ":" + deviceId;
        redisTemplate.delete(key);
    }

    public void storePasswordResetToken(String email, String rawToken) {
        String key = "reset:" + email;
        String hash = hashToken(rawToken);
        @SuppressWarnings("null")
        Duration ttl = Duration.ofMinutes(RESET_TOKEN_TTL_MINUTES);
        redisTemplate.opsForValue().set(key, hash, ttl);
    }

    public boolean validatePasswordResetToken(String email, String rawToken) {
        String key = "reset:" + email;
        String storedHash = redisTemplate.opsForValue().get(key);
        if (storedHash == null) {
            return false;
        }
        String incomingHash = hashToken(rawToken);
        if (storedHash.equals(incomingHash)) {
            redisTemplate.delete(key); // Use only once
            return true;
        }
        return false;
    }

    private String hashToken(String token) {
        if (token == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
