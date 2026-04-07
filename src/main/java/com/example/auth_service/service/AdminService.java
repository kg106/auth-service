package com.example.auth_service.service;

import com.example.auth_service.dto.UserFilterDTO;
import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.ResourceNotFoundException;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.mapper.UserMapper;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.repository.UserSpecification;
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
public class AdminService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<UserResponseDTO> getUsersInTenant(UUID tenantId, UserFilterDTO filter) {
        log.info("Admin request: Fetching users for tenant {} with filter: {}", tenantId, filter);
        filter.setTenantId(tenantId);
        Specification<User> spec = UserSpecification.withFilter(filter);
        List<User> users = userRepository.findAll(spec);
        log.info("Admin: Found {} users for tenant {}", users.size(), tenantId);
        return users.stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserStatusInTenant(UUID tenantId, UUID userId, String status, Boolean isActive) {
        log.info("Admin request: Updating status for user {} in tenant {}: status={}, isActive={}", userId, tenantId, status, isActive);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Admin: User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found");
                });
        
        if (!user.getTenant().getId().equals(tenantId)) {
            log.error("Admin at tenant {} attempted to update user {} belonging to different tenant", tenantId, userId);
            throw new UnauthorizedException("Access denied: User does not belong to your organization");
        }

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
            log.info("Admin: User {} status updated successfully", user.getEmail());
        } else {
            log.info("Admin: No changes detected for user {}", user.getEmail());
        }
    }
}
