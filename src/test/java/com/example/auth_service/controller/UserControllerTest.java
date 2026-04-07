package com.example.auth_service.controller;

import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this test
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser
    void testGetUserById_Success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponseDTO userResponse = UserResponseDTO.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .roleName("USER")
                .tenantName("Test Tenant")
                .isActive(true)
                .status("ACTIVE")
                .build();

        when(userService.getUserById(userId)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.roleName").value("USER"))
                .andExpect(jsonPath("$.tenantName").value("Test Tenant"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).getUserById(userId);
    }

    @Test
    @WithMockUser
    void testGetUserById_NotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenThrow(new RuntimeException("User not found with id: " + userId));

        mockMvc.perform(get("/api/v1/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // GlobalExceptionHandler returns 401 for RuntimeException

        verify(userService, times(1)).getUserById(userId);
    }
}
