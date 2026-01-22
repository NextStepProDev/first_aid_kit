package com.firstaid.service;

import com.firstaid.controller.dto.auth.DeleteAccountRequest;
import com.firstaid.controller.dto.auth.JwtResponse;
import com.firstaid.controller.dto.auth.LoginRequest;
import com.firstaid.controller.dto.auth.RefreshTokenRequest;
import com.firstaid.controller.dto.auth.RegisterRequest;
import com.firstaid.domain.exception.InvalidPasswordException;
import com.firstaid.infrastructure.database.entity.RoleEntity;
import com.firstaid.infrastructure.database.entity.UserEntity;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.database.repository.RoleRepository;
import com.firstaid.infrastructure.database.repository.UserRepository;
import com.firstaid.infrastructure.email.EmailService;
import com.firstaid.infrastructure.security.CurrentUserService;
import com.firstaid.infrastructure.security.CustomUserDetails;
import com.firstaid.infrastructure.security.CustomUserDetailService;
import com.firstaid.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DrugRepository drugRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailService userDetailService;
    private final CurrentUserService currentUserService;
    private final EmailService emailService;

    public JwtResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            log.info("User logged in successfully: {}", request.getEmail());

            return JwtResponse.of(
                    accessToken,
                    refreshToken,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getEmail()
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
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

        UserEntity user = UserEntity.builder()
                .userName(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .active(true)
                .role(Set.of(userRole))
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

        log.info("Token refreshed for user: {}", username);

        return JwtResponse.of(
                newAccessToken,
                refreshToken, // Return the same refresh token
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
        try {
            String subject = "Welcome to First Aid Kit!";
            String body = buildWelcomeEmailBody(user.getName() != null ? user.getName() : user.getUserName());
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", user.getEmail(), e.getMessage());
        }
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
}
