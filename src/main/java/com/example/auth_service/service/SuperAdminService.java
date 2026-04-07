package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.Tenant;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.ResourceNotFoundException;
import com.example.auth_service.mapper.TenantMapper;
import com.example.auth_service.mapper.UserMapper;
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
    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;

    public SuperAdminService(UserRepository userRepository, TenantRepository tenantRepository, 
                            UserMapper userMapper, TenantMapper tenantMapper) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.userMapper = userMapper;
        this.tenantMapper = tenantMapper;
    }

    public List<UserResponseDTO> getAllUsers(UserFilterDTO filter) {
        log.info("Super Admin request: Fetching all users with filter: {}", filter);
        Specification<User> spec = UserSpecification.withFilter(filter);
        List<User> users = userRepository.findAll(spec);
        log.info("Super Admin: Found {} users", users.size());
        return users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TenantResponseDTO> getAllTenants(TenantFilterDTO filter) {
        log.info("Super Admin request: Fetching all tenants with filter: {}", filter);
        Specification<Tenant> spec = TenantSpecification.withFilter(filter);
        List<Tenant> tenants = tenantRepository.findAll(spec);
        log.info("Super Admin: Found {} tenants", tenants.size());
        return tenants.stream()
                .map(tenantMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserStatus(UUID userId, String status, Boolean isActive) {
        log.info("Super Admin request: Updating status for user {}: status={}, isActive={}", userId, status, isActive);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Super Admin: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
        
        boolean changed = false;
        if (status != null && !status.equals(user.getStatus())) {
            log.debug("Updating status for user {}: {} -> {}", user.getEmail(), user.getStatus(), status);
            user.setStatus(status);
            changed = true;
        }
        if (isActive != null && !isActive.equals(user.getIsActive())) {
            log.debug("Updating isActive for user {}: {} -> {}", user.getEmail(), user.getIsActive(), isActive);
            user.setIsActive(isActive);
            changed = true;
        }

        if (changed) {
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Super Admin: User {} status updated successfully", user.getEmail());
        }
    }

    @Transactional
    public void updateTenantStatus(UUID tenantId, String status, Boolean isActive) {
        log.info("Super Admin request: Updating status for tenant {}: status={}, isActive={}", tenantId, status, isActive);
        @SuppressWarnings("null")
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> {
                    log.error("Super Admin: Tenant not found with ID: {}", tenantId);
                    return new ResourceNotFoundException("Tenant not found");
                });
        
        boolean changed = false;
        if (status != null && !status.equals(tenant.getStatus())) {
            log.debug("Updating status for tenant {}: {} -> {}", tenant.getName(), tenant.getStatus(), status);
            tenant.setStatus(status);
            changed = true;
        }
        if (isActive != null && !isActive.equals(tenant.getIsActive())) {
            log.debug("Updating isActive for tenant {}: {} -> {}", tenant.getName(), tenant.getIsActive(), isActive);
            tenant.setIsActive(isActive);
            changed = true;
        }

        if (changed) {
            tenant.setUpdatedAt(LocalDateTime.now());
            tenantRepository.save(tenant);
            log.info("Super Admin: Tenant {} status updated successfully", tenant.getName());
        }
    }
}
