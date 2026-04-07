package com.example.auth_service.service;

import com.example.auth_service.dto.UserFilterDTO;
import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.ResourceNotFoundException;
import com.example.auth_service.exception.UnauthorizedException;
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

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponseDTO> getUsersInTenant(UUID tenantId, UserFilterDTO filter) {
        log.info("Admin: Fetching users for tenant {} with filter: {}", tenantId, filter);
        filter.setTenantId(tenantId);
        Specification<User> spec = UserSpecification.withFilter(filter);
        return userRepository.findAll(spec).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserStatusInTenant(UUID tenantId, UUID userId, String status, Boolean isActive) {
        log.info("Admin: Updating status for user {} in tenant {}: status={}, isActive={}", userId, tenantId, status, isActive);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        if (!user.getTenant().getId().equals(tenantId)) {
            log.error("Admin try to update user {} not in their tenant {}", userId, tenantId);
            throw new UnauthorizedException("User does not belong to this organization");
        }

        if (status != null) user.setStatus(status);
        if (isActive != null) user.setIsActive(isActive);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
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
}
