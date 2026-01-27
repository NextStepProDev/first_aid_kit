package com.firstaidkit.service;

import com.firstaidkit.controller.dto.auth.*;
import com.firstaidkit.domain.exception.AccountLockedException;
import com.firstaidkit.domain.exception.InvalidPasswordException;
import com.firstaidkit.infrastructure.database.entity.RoleEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.DrugRepository;
import com.firstaidkit.infrastructure.database.repository.RoleRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.infrastructure.email.EmailService;
import com.firstaidkit.infrastructure.security.CurrentUserService;
import com.firstaidkit.infrastructure.security.CustomUserDetailService;
import com.firstaidkit.infrastructure.security.CustomUserDetails;
import com.firstaidkit.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DrugRepository drugRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailService userDetailService;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Transactional
    public JwtResponse login(LoginRequest request) {
        // Check if account is locked before attempting authentication
        UserEntity existingUser = userRepository.findByEmail(request.getEmail());
        if (existingUser != null && existingUser.isAccountLocked()) {
            log.warn("Login attempt on locked account: {}", request.getEmail());
            throw new AccountLockedException("Account is locked. Please try again later.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            // Reset failed attempts on successful login
            resetFailedAttempts(userDetails.getUserId());
            updateLastLogin(userDetails.getUserId());

            log.info("User logged in successfully: {}", request.getEmail());

            return JwtResponse.of(
                    accessToken,
                    refreshToken,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getEmail()
            );
        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    private void handleFailedLogin(String email) {
        UserEntity user = userRepository.findByEmail(email);
        if (user != null) {
            user.incrementFailedAttempts();

            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                log.warn("Account locked due to {} failed attempts: {}", MAX_FAILED_ATTEMPTS, email);
            }

            userRepository.save(user);
            log.warn("Failed login attempt {} for user: {}", user.getFailedLoginAttempts(), email);
        }
    }

    private void resetFailedAttempts(Integer userId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.resetFailedAttempts();
                userRepository.save(user);
            }
        });
    }

    private void updateLastLogin(Integer userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(OffsetDateTime.now());
            userRepository.save(user);
        });
    }

    @Transactional
    public JwtResponse register(RegisterRequest request) {
        if (userRepository.existsByUserName(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        RoleEntity userRole = roleRepository.findByRole("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));

        Set<RoleEntity> roles = new HashSet<>();
        roles.add(userRole);

        // Auto-grant ADMIN role if registering with configured admin email
        if (request.getEmail().equalsIgnoreCase(adminEmail)) {
            roleRepository.findByRole("ADMIN").ifPresent(adminRole -> {
                roles.add(adminRole);
                log.info("Granted ADMIN role to user with configured admin email: {}", request.getEmail());
            });
        }

        UserEntity user = UserEntity.builder()
                .userName(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .active(true)
                .role(roles)
                .createdAt(OffsetDateTime.now())
                .build();

        UserEntity savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUserName());

        sendWelcomeEmail(savedUser);

        // Auto-login after registration
        return login(LoginRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build());
    }

    public JwtResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Token is not a refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailService.loadUserByUsername(username);

        String newAccessToken = jwtTokenProvider.generateAccessTokenFromUserDetails(userDetails);
        String newRefreshToken = jwtTokenProvider.generateRefreshTokenFromUserDetails(userDetails);

        log.info("Token refreshed with rotation for user: {}", username);

        return JwtResponse.of(
                newAccessToken,
                newRefreshToken,
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getEmail()
        );
    }

    @Transactional
    @CacheEvict(value = {"drugById", "drugsSearch", "drugStatistics"}, allEntries = true)
    public void deleteAccount(DeleteAccountRequest request) {
        Integer userId = currentUserService.getCurrentUserId();
        String userEmail = currentUserService.getCurrentUserEmail();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password provided for account deletion: {}", userEmail);
            throw new InvalidPasswordException("Invalid password");
        }

        log.info("Deleting account for user: {}", userEmail);

        drugRepository.deleteAllByOwnerUserId(userId);
        log.info("Deleted all drugs for user: {}", userEmail);

        userRepository.delete(user);
        log.info("Deleted user account: {}", userEmail);

        SecurityContextHolder.clearContext();
    }

    private void sendWelcomeEmail(UserEntity user) {
        String subject = "Welcome to First Aid Kit!";
        String body = buildWelcomeEmailBody(user.getName() != null ? user.getName() : user.getUserName());
        emailService.sendEmailAsync(user.getEmail(), subject, body);
    }

    private String buildWelcomeEmailBody(String name) {
        return """
            Hello %s,

            Welcome to First Aid Kit! Your account has been created successfully.

            With First Aid Kit, you can:
            • Track your medications and their expiry dates
            • Get alerts before your medicines expire
            • Manage your personal first aid supplies

            Start by adding your first medication to your kit.

            If you have any questions, feel free to reach out to our support team.

            Stay healthy!
            The First Aid Kit Team
            """.formatted(name);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        Integer userId = currentUserService.getCurrentUserId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        Set<String> roles = user.getRole().stream()
                .map(RoleEntity::getRole)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUserName())
                .email(user.getEmail())
                .name(user.getName())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        Integer userId = currentUserService.getCurrentUserId();
        String currentUsername = currentUserService.getCurrentUsername();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Check if username is being changed and if the new one already exists
        if (!currentUsername.equals(request.username()) && userRepository.existsByUserName(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        user.setUserName(request.username());
        user.setName(request.name());

        UserEntity savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", savedUser.getEmail());

        Set<String> roles = savedUser.getRole().stream()
                .map(RoleEntity::getRole)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUserName())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .roles(roles)
                .createdAt(savedUser.getCreatedAt())
                .lastLogin(savedUser.getLastLogin())
                .build();
    }
}
