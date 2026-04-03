package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.Tenant;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import com.example.auth_service.repository.RoleRepository;
import com.example.auth_service.repository.TenantRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenStoreService tokenStoreService;

    public AuthService(UserRepository userRepository, TenantRepository tenantRepository,
                       RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider, TokenStoreService tokenStoreService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenStoreService = tokenStoreService;
    }

    @Transactional
    public void register(RegistrationRequest request) {
        Optional<Tenant> tenantOpt = tenantRepository.findByTenantIdStr(request.getTenantIdStr());
        if (tenantOpt.isEmpty() || !Boolean.TRUE.equals(tenantOpt.get().getIsActive())) {
            throw new RuntimeException("Invalid or inactive tenant");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User already exists with email: " + request.getEmail());
        }

        UserRole defaultRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(
                        UserRole.builder().name("USER").build()
                ));

        User user = User.builder()
                .tenant(tenantOpt.get())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getFirstname() + " " + request.getLastname())
                .mobileNumber(request.getMobileNumber())
                .role(defaultRole)
                .tenantName(tenantOpt.get().getName())
                .isActive(true)
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailWithRolesAndTenant(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getIsActive()) || !"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active");
        }

        Tenant tenant = user.getTenant();
        if (tenant == null || !Boolean.TRUE.equals(tenant.getIsActive()) || !"ACTIVE".equals(tenant.getStatus())) {
            throw new RuntimeException("Tenant is not active");
        }

        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : UUID.randomUUID().toString();
        
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();

        tokenStoreService.storeRefreshToken(user.getId(), deviceId, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .deviceId(deviceId)
                .userId(user.getId())
                .build();
    }

    public AuthResponse refresh(RefreshRequest request) {
        boolean isValid = tokenStoreService.validateAndRotateRefreshToken(
                request.getUserId(), request.getDeviceId(), request.getRefreshToken()
        );

        if (!isValid) {
            throw new RuntimeException("Invalid refresh token. Please login again.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!Boolean.TRUE.equals(user.getIsActive()) || !"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("User is not active");
        }

        Tenant tenant = user.getTenant();
        if (tenant == null || !Boolean.TRUE.equals(tenant.getIsActive())) {
             throw new RuntimeException("Tenant is not active");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = UUID.randomUUID().toString();

        tokenStoreService.storeRefreshToken(user.getId(), request.getDeviceId(), newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .deviceId(request.getDeviceId())
                .userId(user.getId())
                .build();
    }

    public void logout(LogoutRequest request) {
        tokenStoreService.deleteSession(request.getUserId(), request.getDeviceId());
        tokenStoreService.blacklistAccessToken(request.getAccessTokenJwtId());
    }
}
