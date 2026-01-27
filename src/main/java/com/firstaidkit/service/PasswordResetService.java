package com.firstaidkit.service;

import com.firstaidkit.controller.dto.auth.ChangePasswordRequest;
import com.firstaidkit.controller.dto.auth.ForgotPasswordRequest;
import com.firstaidkit.controller.dto.auth.ResetPasswordRequest;
import com.firstaidkit.domain.exception.InvalidPasswordException;
import com.firstaidkit.domain.exception.InvalidTokenException;
import com.firstaidkit.domain.exception.PasswordMismatchException;
import com.firstaidkit.infrastructure.database.entity.PasswordResetTokenEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.PasswordResetTokenRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.infrastructure.email.EmailService;
import com.firstaidkit.infrastructure.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int TOKEN_EXPIRATION_MINUTES = 30;
    private static final int TOKEN_LENGTH_BYTES = 32; // 256 bits

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest request, String baseUrl) {
        String email = request.getEmail();
        log.info("Password reset requested for email: {}", email);

        UserEntity user = userRepository.findByEmail(email);

        if (user == null) {
            // Don't reveal whether email exists - protection against enumeration
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        // Delete any existing tokens for this user
        tokenRepository.deleteAllByUserId(user.getUserId());

        // Generate secure token
        String token = generateSecureToken();

        // Create and save token entity
        PasswordResetTokenEntity tokenEntity = PasswordResetTokenEntity.builder()
                .token(token)
                .user(user)
                .expiresAt(OffsetDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES))
                .createdAt(OffsetDateTime.now())
                .build();

        tokenRepository.save(tokenEntity);
        log.info("Password reset token created for user: {}", user.getUserName());

        // Send email with reset link
        sendPasswordResetEmail(user, token, baseUrl);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        // Find and validate token
        PasswordResetTokenEntity tokenEntity = tokenRepository.findValidToken(
                request.getToken(),
                OffsetDateTime.now()
        ).orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        UserEntity user = tokenEntity.getUser();

        // Ensure new password is different from old
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new PasswordMismatchException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark token as used
        tokenEntity.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(tokenEntity);

        log.info("Password reset successful for user: {}", user.getUserName());

        // Send confirmation email
        sendPasswordChangedConfirmationEmail(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // Validate passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        Integer userId = currentUserService.getCurrentUserId();
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        // Ensure new password is different from old
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new PasswordMismatchException("New password must be different from current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getUserName());

        // Send confirmation email
        sendPasswordChangedConfirmationEmail(user);
    }

    public boolean isTokenValid(String token) {
        return tokenRepository.findValidToken(token, OffsetDateTime.now()).isPresent();
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = tokenRepository.deleteExpiredTokens(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired password reset tokens", deleted);
        }
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void sendPasswordResetEmail(UserEntity user, String token, String baseUrl) {
        try {
            // Wybieramy bazowy adres (Frontend)
            String effectiveBaseUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : baseUrl;

            // Budujemy link bezpiecznie
            String resetLink = UriComponentsBuilder.fromUriString(effectiveBaseUrl) // To zadziała na 100%
                    .path("/reset-password")
                    .queryParam("token", token)
                    .build()
                    .toUriString();

            String subject = "Password Reset Request - First Aid Kit";
            String body = buildPasswordResetEmailBody(
                    user.getName() != null ? user.getName() : user.getUserName(),
                    resetLink
            );
            emailService.sendEmail(user.getEmail(), subject, body);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void sendPasswordChangedConfirmationEmail(UserEntity user) {
        try {
            String subject = "Password Changed - First Aid Kit";
            String body = buildPasswordChangedEmailBody(
                    user.getName() != null ? user.getName() : user.getUserName()
            );
            emailService.sendEmail(user.getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send password change confirmation email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildPasswordResetEmailBody(String name, String resetLink) {
        return """
            Hello %s,

            We received a request to reset your password for your First Aid Kit account.

            Click the link below to reset your password:
            %s

            This link will expire in %d minutes.

            If you did not request a password reset, please ignore this email. Your password will remain unchanged.

            Stay healthy!
            The First Aid Kit Team
            """.formatted(name, resetLink, TOKEN_EXPIRATION_MINUTES);
    }

    private String buildPasswordChangedEmailBody(String name) {
        return """
            Hello %s,

            Your password has been successfully changed.

            If you did not make this change, please contact our support team immediately.

            Stay healthy!
            The First Aid Kit Team
            """.formatted(name);
    }
}
