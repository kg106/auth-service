package com.example.auth_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

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
        String rawToken = "my-secret-token";
        tokenStoreService.storeRefreshToken(1L, "dev-1", rawToken);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("session:1:dev-1"), hashCaptor.capture(), any(Duration.class));

        assertNotNull(hashCaptor.getValue());
        assertNotEquals(rawToken, hashCaptor.getValue()); // Ensure it's safely hashed
    }

    @Test
    void testValidateAndRotateRefreshToken_ValidToken_ReturnsTrueAndDeletes() {
        String rawToken = "my-secret-token";
        tokenStoreService.storeRefreshToken(1L, "dev-1", rawToken);
        
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("session:1:dev-1"), hashCaptor.capture(), any(Duration.class));
        String expectedHash = hashCaptor.getValue();

        when(valueOperations.get("session:1:dev-1")).thenReturn(expectedHash);

        boolean isValid = tokenStoreService.validateAndRotateRefreshToken(1L, "dev-1", rawToken);
        
        assertTrue(isValid);
        verify(redisTemplate).delete("session:1:dev-1"); // Essential rotation check
    }

    @Test
    void testValidateAndRotateRefreshToken_InvalidToken_ReturnsFalseAndDeletes() {
        when(valueOperations.get("session:1:dev-1")).thenReturn("differentHash");

        boolean isValid = tokenStoreService.validateAndRotateRefreshToken(1L, "dev-1", "bad-token");
        
        assertFalse(isValid);
        verify(redisTemplate).delete("session:1:dev-1"); // Safety measure for compromised sessions
    }
}
