package com.firstaid.service;

import com.firstaid.controller.dto.admin.UserResponse;
import com.firstaid.domain.exception.InvalidPasswordException;
import com.firstaid.domain.exception.UserNotFoundException;
import com.firstaid.infrastructure.database.entity.RoleEntity;
import com.firstaid.infrastructure.database.entity.UserEntity;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.database.repository.UserRepository;
import com.firstaid.infrastructure.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final DrugRepository drugRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserResponse);
    }

    private UserResponse mapToUserResponse(UserEntity user) {
        Set<String> roles = user.getRole() != null
                ? user.getRole().stream()
                        .map(RoleEntity::getRole)
                        .collect(Collectors.toSet())
                : Set.of();

        return new UserResponse(
                user.getUserId(),
                user.getUserName(),
                user.getEmail(),
                user.getName(),
                user.getActive(),
                roles,
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }

    @Transactional
    @CacheEvict(value = {"drugById", "drugsSearch", "drugStatistics"}, allEntries = true)
    public void deleteUser(Integer userId, String adminPassword) {
        Integer adminId = currentUserService.getCurrentUserId();
        String adminEmail = currentUserService.getCurrentUserEmail();

        UserEntity admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalStateException("Admin user not found"));

        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            log.warn("Invalid password provided by admin {} for user deletion", adminEmail);
            throw new InvalidPasswordException("Invalid password");
        }

        if (userId.equals(adminId)) {
            throw new IllegalArgumentException("Cannot delete your own account through admin panel");
        }

        UserEntity userToDelete = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        String deletedUserEmail = userToDelete.getEmail();

        drugRepository.deleteAllByOwnerUserId(userId);
        log.info("Admin {} deleted all drugs for user: {}", adminEmail, deletedUserEmail);

        userRepository.delete(userToDelete);
        log.info("Admin {} deleted user account: {}", adminEmail, deletedUserEmail);
    }
}
