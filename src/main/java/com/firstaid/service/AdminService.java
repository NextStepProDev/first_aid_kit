package com.firstaid.service;

import com.firstaid.controller.dto.admin.BroadcastEmailRequest;
import com.firstaid.controller.dto.admin.UserResponse;
import com.firstaid.domain.exception.InvalidPasswordException;
import com.firstaid.domain.exception.UserNotFoundException;
import com.firstaid.infrastructure.database.entity.RoleEntity;
import com.firstaid.infrastructure.database.entity.UserEntity;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.database.repository.UserRepository;
import com.firstaid.infrastructure.email.EmailService;
import com.firstaid.infrastructure.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final EmailService emailService;

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

    @Transactional(readOnly = true)
    public int broadcastEmail(BroadcastEmailRequest request) {
        String adminEmail = currentUserService.getCurrentUserEmail();
        List<UserEntity> users = userRepository.findAll();

        int sentCount = 0;
        for (UserEntity user : users) {
            if (user.getActive()) {
                emailService.sendEmailAsync(user.getEmail(), request.subject(), request.message());
                sentCount++;
            }
        }

        log.info("Admin {} sent broadcast email '{}' to {} users", adminEmail, request.subject(), sentCount);
        return sentCount;
    }

    @Transactional(readOnly = true)
    public String exportEmailsCsv() {
        String adminEmail = currentUserService.getCurrentUserEmail();
        List<UserEntity> users = userRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("email,name,username,active,created_at\n");

        for (UserEntity user : users) {
            csv.append(escapeCsv(user.getEmail())).append(",");
            csv.append(escapeCsv(user.getName() != null ? user.getName() : "")).append(",");
            csv.append(escapeCsv(user.getUserName())).append(",");
            csv.append(user.getActive()).append(",");
            csv.append(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "").append("\n");
        }

        log.info("Admin {} exported {} user emails to CSV", adminEmail, users.size());
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
