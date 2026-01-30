package com.firstaidkit.service;

import com.firstaidkit.domain.exception.InvalidTokenException;
import com.firstaidkit.infrastructure.database.entity.EmailVerificationTokenEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.EmailVerificationTokenRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final int TOKEN_LENGTH_BYTES = 32;

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.frontend.url:}")
    private String frontendUrl;

    @Transactional
    public void createVerificationToken(UserEntity user) {
        tokenRepository.deleteAllByUserId(user.getUserId());

        String token = generateSecureToken();

        EmailVerificationTokenEntity tokenEntity = EmailVerificationTokenEntity.builder()
                .token(token)
                .user(user)
                .expiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS))
                .createdAt(OffsetDateTime.now())
                .build();

        tokenRepository.save(tokenEntity);
        log.info("Email verification token created for user: {}", user.getUserName());

        String verificationUrl = buildVerificationUrl(token);
        sendVerificationEmail(user, verificationUrl);
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationTokenEntity tokenEntity = tokenRepository.findValidToken(
                token,
                OffsetDateTime.now()
        ).orElseThrow(() -> new InvalidTokenException("Link aktywacyjny jest nieprawidlowy lub wygasl."));

        UserEntity user = tokenEntity.getUser();
        user.setActive(true);
        userRepository.save(user);

        tokenEntity.setVerifiedAt(OffsetDateTime.now());
        tokenRepository.save(tokenEntity);

        log.info("Email verified for user: {}", user.getUserName());
    }

    @Transactional
    public void resendVerification(String email) {
        UserEntity user = userRepository.findByEmail(email);

        if (user == null) {
            log.warn("Resend verification requested for non-existent email: {}", email);
            return;
        }

        if (user.getActive()) {
            log.warn("Resend verification requested for already active user: {}", email);
            return;
        }

        createVerificationToken(user);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = tokenRepository.deleteExpiredTokens(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired email verification tokens", deleted);
        }
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildVerificationUrl(String token) {
        String baseUrl = (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : "http://localhost:3000";
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path("/verify-email")
                .queryParam("token", token)
                .build()
                .toUriString();
    }

    private void sendVerificationEmail(UserEntity user, String verificationUrl) {
        try {
            String subject = "\u2709\uFE0F Potwierdz swoj adres email - First Aid Kit";
            String body = buildVerificationEmailBody(
                    user.getName() != null ? user.getName() : user.getUserName(),
                    verificationUrl
            );
            emailService.sendEmail(user.getEmail(), subject, body);
            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String buildVerificationEmailBody(String name, String verificationUrl) {
        return """
            \uD83D\uDC4B Hello %s,

            \u2764\uFE0F Thank you for registering with First Aid Kit Manager! We're happy to have you on board.

            \uD83D\uDD17 Please click the link below to verify your email address and activate your account:
            %s

            \u23F3 This link will expire in %d hours.

            If you did not create an account, please ignore this email.

            \uD83D\uDC9A Stay healthy!
            The First Aid Kit Team \uD83C\uDFE5
            """.formatted(name, verificationUrl, TOKEN_EXPIRATION_HOURS);
    }
}
