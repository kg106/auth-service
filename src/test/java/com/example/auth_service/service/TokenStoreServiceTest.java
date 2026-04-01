package com.example.auth_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TokenStoreServiceTest {

    private TokenStoreService tokenStoreService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenStoreService = new TokenStoreService(redisTemplate);
    }

    @Test
    void testStoreRefreshToken_HashesToken() {
        UUID userId = UUID.randomUUID();
        String rawToken = "my-secret-token";
        tokenStoreService.storeRefreshToken(userId, "dev-1", rawToken);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("session:" + userId + ":dev-1"), hashCaptor.capture(), any(Duration.class));

        assertNotNull(hashCaptor.getValue());
        assertNotEquals(rawToken, hashCaptor.getValue()); // Ensure it's safely hashed
    }

    @Test
    void testStoreRefreshToken() {
        UUID userId = UUID.randomUUID();
        tokenStoreService.storeRefreshToken(userId, "device-1", "my-refresh-token");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("session:" + userId + ":device-1"), hashCaptor.capture(), any(Duration.class));
        assertNotNull(hashCaptor.getValue());
    }

    @Test
    void testValidateAndRotateRefreshToken_Success() {
        UUID userId = UUID.randomUUID();
        String rawToken = "valid-refresh-token";
        
        // 1. Store a token
        tokenStoreService.storeRefreshToken(userId, "device-1", rawToken);

        // 2. Capture the securely hashed token that was just saved
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("session:" + userId + ":device-1"), hashCaptor.capture(), any(Duration.class));
        String expectedHash = hashCaptor.getValue();
        
        // 3. Mock the behavior for retrieving
        when(valueOperations.get("session:" + userId + ":device-1")).thenReturn(expectedHash);

        // 4. Validate the SAME token
        boolean isValid = tokenStoreService.validateAndRotateRefreshToken(userId, "device-1", rawToken);
        
        assertTrue(isValid, "Token should be valid");
        verify(redisTemplate).delete("session:" + userId + ":device-1");
    }

    @Test
    void testValidateAndRotateRefreshToken_Failure() {
        UUID userId = UUID.randomUUID();
        
        // Mock the behavior for mismatch
        when(valueOperations.get("session:" + userId + ":device-1")).thenReturn("differentHash");

        // 2. Try to validate a WRONG token
        boolean isValid = tokenStoreService.validateAndRotateRefreshToken(userId, "device-1", "wrong-token");
        
        assertFalse(isValid, "Token should be invalid");
        verify(redisTemplate).delete("session:" + userId + ":device-1");
    }
}
