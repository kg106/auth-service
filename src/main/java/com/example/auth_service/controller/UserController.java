package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@Tag(name = "2. User Profile & Account", description = "Endpoints for retrieving and updating user profile information.")
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Returns the user information for the given user ID.")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable UUID userId) {
        UserResponseDTO userResponse = userService.getUserById(userId);
        return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getCurrentUser(Principal principal) {
        log.info("Request to get current user profile: {}", principal.getName());
        // Since my filter sets Subject as Email, I'll find by email
        UserResponseDTO user = userService.getUserByEmail(principal.getName());
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserResponseDTO> updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        log.info("Request to update profile for user: {}", principal.getName());
        UserResponseDTO updatedUser = userService.updateProfileByEmail(principal.getName(), request);
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Request to change password for user: {}", principal.getName());
        userService.changePasswordByEmail(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
