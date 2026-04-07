package com.example.auth_service.controller;

import com.example.auth_service.dto.*;
import com.example.auth_service.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/super-admin")
@Tag(name = "Super Admin", description = "Endpoints for Super Admin to manage users and organizations.")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users with filters", description = "Returns a filtered list of all users.")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers(UserFilterDTO filter) {
        return ResponseEntity.ok(superAdminService.getAllUsers(filter));
    }

    @GetMapping("/tenants")
    @Operation(summary = "Get all tenants with filters", description = "Returns a filtered list of all organizations.")
    public ResponseEntity<List<TenantResponseDTO>> getAllTenants(TenantFilterDTO filter) {
        return ResponseEntity.ok(superAdminService.getAllTenants(filter));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Update user status", description = "Updates the status and active flag of a user.")
    public ResponseEntity<String> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive) {
        superAdminService.updateUserStatus(userId, status, isActive);
        return ResponseEntity.ok("User status updated successfully");
    }

    @PatchMapping("/tenants/{tenantId}/status")
    @Operation(summary = "Update tenant status", description = "Updates the status and active flag of an organization.")
    public ResponseEntity<String> updateTenantStatus(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive) {
        superAdminService.updateTenantStatus(tenantId, status, isActive);
        return ResponseEntity.ok("Tenant status updated successfully");
    }
}
