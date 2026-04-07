package com.example.auth_service.controller;

import com.example.auth_service.dto.UserFilterDTO;
import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.service.AdminService;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "3. Admin - Organization Management", description = "Endpoints for Admin to manage users under its organization.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get users in organization", description = "Returns a filtered list of users belonging to the admin's organization.")
    public ResponseEntity<List<UserResponseDTO>> getUsersInOrganization(UserFilterDTO filter, Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        return ResponseEntity.ok(adminService.getUsersInTenant(tenantId, filter));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Update user status in organization", description = "Updates the status and active flag of a user in the admin's organization.")
    public ResponseEntity<String> updateUserStatusInOrganization(
            @PathVariable UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive,
            Authentication authentication) {
        UUID tenantId = getTenantIdFromAuth(authentication);
        adminService.updateUserStatusInTenant(tenantId, userId, status, isActive);
        return ResponseEntity.ok("User status updated successfully");
    }

    private UUID getTenantIdFromAuth(Authentication authentication) {
        Claims claims = (Claims) authentication.getDetails();
        String tenantIdStr = claims.get("tenantId", String.class);
        if (tenantIdStr == null) {
            throw new RuntimeException("Tenant ID not found in token");
        }
        return UUID.fromString(tenantIdStr);
    }
}
