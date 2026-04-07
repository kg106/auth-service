package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.Tenant;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.ResourceNotFoundException;
import com.example.auth_service.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SuperAdminService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public SuperAdminService(UserRepository userRepository, TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    public List<UserResponseDTO> getAllUsers(UserFilterDTO filter) {
        log.info("Super Admin: Fetching all users with filter: {}", filter);
        Specification<User> spec = UserSpecification.withFilter(filter);
        return userRepository.findAll(spec).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public List<TenantResponseDTO> getAllTenants(TenantFilterDTO filter) {
        log.info("Super Admin: Fetching all tenants with filter: {}", filter);
        Specification<Tenant> spec = TenantSpecification.withFilter(filter);
        return tenantRepository.findAll(spec).stream()
                .map(this::mapToTenantResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserStatus(UUID userId, String status, Boolean isActive) {
        log.info("Super Admin: Updating status for user {}: status={}, isActive={}", userId, status, isActive);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (status != null) user.setStatus(status);
        if (isActive != null) user.setIsActive(isActive);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public void updateTenantStatus(UUID tenantId, String status, Boolean isActive) {
        log.info("Super Admin: Updating status for tenant {}: status={}, isActive={}", tenantId, status, isActive);
        @SuppressWarnings("null")
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with id: " + tenantId));
        
        if (status != null) tenant.setStatus(status);
        if (isActive != null) tenant.setIsActive(isActive);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
    }

    private UserResponseDTO mapToUserResponse(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .mobileNumber(user.getMobileNumber())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .tenantName(user.getTenantName())
                .dob(user.getDob())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private TenantResponseDTO mapToTenantResponse(Tenant tenant) {
        return TenantResponseDTO.builder()
                .id(tenant.getId())
                .tenantIdStr(tenant.getTenantIdStr())
                .name(tenant.getName())
                .email(tenant.getEmail())
                .plan(tenant.getPlan())
                .status(tenant.getStatus())
                .isActive(tenant.getIsActive())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
