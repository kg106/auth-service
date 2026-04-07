package com.example.auth_service.service;

import com.example.auth_service.dto.*;
import com.example.auth_service.entity.User;
import com.example.auth_service.exception.BadRequestException;
import com.example.auth_service.exception.ResourceNotFoundException;
import com.example.auth_service.mapper.UserMapper;
import com.example.auth_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    public UserResponseDTO getUserById(UUID userId) {
        log.info("Fetching user with ID: {}", userId);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User not found with ID: " + userId);
                });

        return userMapper.toResponse(user);
    }

    public UserResponseDTO getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new ResourceNotFoundException("User not found with email: " + email);
                });
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponseDTO updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user ID: {}", userId);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {} during profile update", userId);
                    return new ResourceNotFoundException("User not found");
                });
        return updateProfileInternal(user, request);
    }

    @Transactional
    public UserResponseDTO updateProfileByEmail(String email, UpdateProfileRequest request) {
        log.info("Updating profile for user email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {} during profile update", email);
                    return new ResourceNotFoundException("User not found");
                });
        return updateProfileInternal(user, request);
    }

    private UserResponseDTO updateProfileInternal(User user, UpdateProfileRequest request) {
        if (request.getName() != null) {
            log.debug("Updating name for user {}: {} -> {}", user.getEmail(), user.getName(), request.getName());
            user.setName(request.getName());
        }
        if (request.getMobileNumber() != null) {
            log.debug("Updating mobile number for user {}: {} -> {}", user.getEmail(), user.getMobileNumber(), request.getMobileNumber());
            user.setMobileNumber(request.getMobileNumber());
        }
        if (request.getDob() != null) {
            log.debug("Updating DOB for user {}: {} -> {}", user.getEmail(), user.getDob(), request.getDob());
            user.setDob(request.getDob());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        log.info("Profile updated successfully for user: {}", updatedUser.getEmail());
        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user ID: {}", userId);
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {} during password change", userId);
                    return new ResourceNotFoundException("User not found");
                });
        changePasswordInternal(user, request);
    }

    @Transactional
    public void changePasswordByEmail(String email, ChangePasswordRequest request) {
        log.info("Changing password for user email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {} during password change", email);
                    return new ResourceNotFoundException("User not found");
                });
        changePasswordInternal(user, request);
    }

    private void changePasswordInternal(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.error("Current password mismatch for user: {}", user.getEmail());
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Password changed successfully for user: {}", user.getEmail());
    }
}
