package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_tenants_id")
    private Long id;

    @Column(name = "tennant_id")
    private String tenantIdStr;

    private String name;
    private String email;
    private String plan;
    
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "max_daily_attempts")
    private Integer maxDailyAttempts;

    @Column(name = "allowed_document_types")
    private String allowedDocumentTypes;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "org_code")
    private String orgCode;

    private String status;

    private String subdomain;
    private String createdby;
    private String updatedby;
    private String description;

    @Column(name = "tenant_type")
    private String tenantType;
}
