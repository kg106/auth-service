package com.example.auth_service.mapper;

import com.example.auth_service.dto.TenantResponseDTO;
import com.example.auth_service.entity.Tenant;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper {

    public TenantResponseDTO toResponse(Tenant tenant) {
        if (tenant == null) {
            return null;
        }

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
