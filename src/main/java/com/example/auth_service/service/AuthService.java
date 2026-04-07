package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.Tenant;
import com.example.auth_service.entity.User;
import com.example.auth_service.entity.UserRole;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.ResourceNotFoundException;
import com.example.auth_service.exception.UnauthorizedException;
import com.example.auth_service.repository.RoleRepository;
import com.example.auth_service.repository.TenantRepository;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
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
        log.info("Registering new user with email: {}", request.getEmail());
        Optional<Tenant> tenantOpt = tenantRepository.findByTenantIdStr(request.getTenantIdStr());
        if (tenantOpt.isEmpty() || !Boolean.TRUE.equals(tenantOpt.get().getIsActive())) {
            log.error("Invalid or inactive tenant: {}", request.getTenantIdStr());
            throw new BadRequestException("Invalid or inactive tenant");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.error("User already exists with email: {}", request.getEmail());
            throw new BadRequestException("User already exists with email: " + request.getEmail());
        }

        UserRole defaultRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    log.info("Default role USER not found, creating it");
                    return roleRepository.save(UserRole.builder().name("USER").build());
                });

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
        log.info("User registered successfully: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        User user = userRepository.findByEmailWithRolesAndTenant(request.getEmail())
                .orElseThrow(() -> {
                    log.error("Invalid email for login: {}", request.getEmail());
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.error("Invalid password for login: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive()) || !"ACTIVE".equals(user.getStatus())) {
            log.error("User is not active: {}", request.getEmail());
            throw new UnauthorizedException("User is not active");
        }

        Tenant tenant = user.getTenant();
        if (tenant == null || !Boolean.TRUE.equals(tenant.getIsActive()) || !"ACTIVE".equals(tenant.getStatus())) {
            log.error("Tenant is not active for user: {}", request.getEmail());
            throw new UnauthorizedException("Tenant is not active");
        }

        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : UUID.randomUUID().toString();
        
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();

        tokenStoreService.storeRefreshToken(user.getId(), deviceId, refreshToken);

        log.info("User logged in successfully: {}", request.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .deviceId(deviceId)
                .userId(user.getId())
                .build();
    }

    public AuthResponse refresh(RefreshRequest request) {
        log.info("Refreshing token for user id: {}", request.getUserId());
        boolean isValid = tokenStoreService.validateAndRotateRefreshToken(
                request.getUserId(), request.getDeviceId(), request.getRefreshToken()
        );

        if (!isValid) {
            log.error("Invalid refresh token for user id: {}", request.getUserId());
            throw new UnauthorizedException("Invalid refresh token. Please login again.");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found during refresh: {}", request.getUserId());
                    return new ResourceNotFoundException("User not found");
                });

        if (!Boolean.TRUE.equals(user.getIsActive()) || !"ACTIVE".equals(user.getStatus())) {
            log.error("User is not active during refresh: {}", user.getEmail());
            throw new UnauthorizedException("User is not active");
        }

        Tenant tenant = user.getTenant();
        if (tenant == null || !Boolean.TRUE.equals(tenant.getIsActive())) {
             log.error("Tenant is not active during refresh for user: {}", user.getEmail());
             throw new UnauthorizedException("Tenant is not active");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = UUID.randomUUID().toString();

        tokenStoreService.storeRefreshToken(user.getId(), request.getDeviceId(), newRefreshToken);

        log.info("Token refreshed successfully for user: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .deviceId(request.getDeviceId())
                .userId(user.getId())
                .build();
    }

    public void logout(LogoutRequest request) {
        log.info("Logging out user id: {}", request.getUserId());
        tokenStoreService.deleteSession(request.getUserId(), request.getDeviceId());
        tokenStoreService.blacklistAccessToken(request.getAccessTokenJwtId());
        log.info("User logged out successfully: {}", request.getUserId());
    }
}
