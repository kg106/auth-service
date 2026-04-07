package com.example.auth_service.mapper;

import com.example.auth_service.dto.UserResponseDTO;
import com.example.auth_service.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponseDTO toResponse(User user) {
        if (user == null) {
            return null;
        }

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
