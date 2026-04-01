package com.example.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_users_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_tenants_id")
    private Tenant tenant;

    @Column(name = "auth_user_role_id")
    private Long primaryRoleId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private LocalDate dob;
    
    private String email;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "mobile_number")
    private String mobileNumber;

    private String name;

    @Column(name = "password_hash")
    private String passwordHash;

    private String createdby;

    @Column(name = "ispasswordchanged")
    private Boolean isPasswordChanged;

    private String updatedby;

    private String address;
    private String firstname;
    private String lastname;
    private String username;
    private String city;
    private String role;
    private String status;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "mtom_user_roles",
        schema = "auth",
        joinColumns = @JoinColumn(name = "auth_users_id"),
        inverseJoinColumns = @JoinColumn(name = "auth_user_role_id")
    )
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();
}
