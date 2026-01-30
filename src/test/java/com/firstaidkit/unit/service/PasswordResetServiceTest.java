package com.firstaidkit.unit.service;

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
import com.firstaidkit.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Integer TEST_USER_ID = 1;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String ENCODED_PASSWORD = "encodedOldPassword";

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "frontendUrl", "http://localhost:3000");
    }

    private UserEntity buildUserEntity() {
        return UserEntity.builder()
                .userId(TEST_USER_ID)
                .userName(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .name("Test User")
                .active(true)
                .build();
    }

    // ---------------------- initiatePasswordReset ----------------------
    @Nested
    @DisplayName("initiatePasswordReset")
    class InitiatePasswordReset {

        @Test
        void shouldCreateTokenAndSendEmail() {
            UserEntity user = buildUserEntity();
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(user);

            ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(TEST_EMAIL).build();
            passwordResetService.initiatePasswordReset(request, "http://localhost:8080");

            verify(tokenRepository).deleteAllByUserId(TEST_USER_ID);
            ArgumentCaptor<PasswordResetTokenEntity> captor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getToken()).isNotBlank();
            assertThat(captor.getValue().getUser()).isEqualTo(user);
            assertThat(captor.getValue().getExpiresAt()).isAfter(OffsetDateTime.now());
            verify(emailService).sendEmail(eq(TEST_EMAIL), anyString(), anyString());
        }

        @Test
        void shouldSilentlyReturnWhenEmailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(null);

            ForgotPasswordRequest request = ForgotPasswordRequest.builder().email("unknown@example.com").build();
            passwordResetService.initiatePasswordReset(request, "http://localhost:8080");

            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        }

        @Test
        void shouldDeletePreviousTokensBeforeCreatingNew() {
            UserEntity user = buildUserEntity();
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(user);

            ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(TEST_EMAIL).build();
            passwordResetService.initiatePasswordReset(request, "http://localhost:8080");

            // deleteAllByUserId should be called before save
            var inOrder = inOrder(tokenRepository);
            inOrder.verify(tokenRepository).deleteAllByUserId(TEST_USER_ID);
            inOrder.verify(tokenRepository).save(any(PasswordResetTokenEntity.class));
        }
    }

    // ---------------------- resetPassword ----------------------
    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        void shouldResetPasswordAndMarkTokenUsed() {
            UserEntity user = buildUserEntity();
            PasswordResetTokenEntity tokenEntity = PasswordResetTokenEntity.builder()
                    .tokenId(1).token("valid-token").user(user)
                    .expiresAt(OffsetDateTime.now().plusMinutes(30))
                    .createdAt(OffsetDateTime.now()).build();

            when(tokenRepository.findValidToken(eq("valid-token"), any(OffsetDateTime.class)))
                    .thenReturn(Optional.of(tokenEntity));
            when(passwordEncoder.matches("newPassword", ENCODED_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token").newPassword("newPassword").confirmPassword("newPassword").build();

            passwordResetService.resetPassword(request);

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPassword()).isEqualTo("encodedNewPassword");

            ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull();
        }

        @Test
        void shouldThrowWhenPasswordsDoNotMatch() {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("token").newPassword("pass1").confirmPassword("pass2").build();

            assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                    .isInstanceOf(PasswordMismatchException.class)
                    .hasMessageContaining("Passwords do not match");
        }

        @Test
        void shouldThrowWhenTokenInvalidOrExpired() {
            when(tokenRepository.findValidToken(eq("expired"), any(OffsetDateTime.class)))
                    .thenReturn(Optional.empty());

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("expired").newPassword("newPass").confirmPassword("newPass").build();

            assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid or expired");
        }

        @Test
        void shouldThrowWhenNewPasswordSameAsOld() {
            UserEntity user = buildUserEntity();
            PasswordResetTokenEntity tokenEntity = PasswordResetTokenEntity.builder()
                    .tokenId(1).token("valid-token").user(user)
                    .expiresAt(OffsetDateTime.now().plusMinutes(30))
                    .createdAt(OffsetDateTime.now()).build();

            when(tokenRepository.findValidToken(eq("valid-token"), any(OffsetDateTime.class)))
                    .thenReturn(Optional.of(tokenEntity));
            when(passwordEncoder.matches("samePassword", ENCODED_PASSWORD)).thenReturn(true);

            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .token("valid-token").newPassword("samePassword").confirmPassword("samePassword").build();

            assertThatThrownBy(() -> passwordResetService.resetPassword(request))
                    .isInstanceOf(PasswordMismatchException.class)
                    .hasMessageContaining("different from current");
        }
    }

    // ---------------------- changePassword ----------------------
    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @BeforeEach
        void setUp() {
            lenient().when(currentUserService.getCurrentUserId()).thenReturn(TEST_USER_ID);
        }

        @Test
        void shouldChangePasswordSuccessfully() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("currentPass", ENCODED_PASSWORD)).thenReturn(true);
            when(passwordEncoder.matches("newPass123", ENCODED_PASSWORD)).thenReturn(false);
            when(passwordEncoder.encode("newPass123")).thenReturn("encodedNewPass");

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("currentPass").newPassword("newPass123").confirmPassword("newPass123").build();

            passwordResetService.changePassword(request);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPassword()).isEqualTo("encodedNewPass");
        }

        @Test
        void shouldThrowWhenCurrentPasswordIncorrect() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongCurrent", ENCODED_PASSWORD)).thenReturn(false);

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("wrongCurrent").newPassword("newPass").confirmPassword("newPass").build();

            assertThatThrownBy(() -> passwordResetService.changePassword(request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("Current password is incorrect");
        }

        @Test
        void shouldThrowWhenNewPasswordSameAsCurrent() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("samePass", ENCODED_PASSWORD)).thenReturn(true);

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("samePass").newPassword("samePass").confirmPassword("samePass").build();

            assertThatThrownBy(() -> passwordResetService.changePassword(request))
                    .isInstanceOf(PasswordMismatchException.class)
                    .hasMessageContaining("different from current");
        }

        @Test
        void shouldThrowWhenConfirmationDoesNotMatch() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("current").newPassword("new1").confirmPassword("new2").build();

            assertThatThrownBy(() -> passwordResetService.changePassword(request))
                    .isInstanceOf(PasswordMismatchException.class)
                    .hasMessageContaining("Passwords do not match");
        }
    }

    // ---------------------- isTokenValid ----------------------
    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        void shouldReturnTrueForValidToken() {
            PasswordResetTokenEntity tokenEntity = PasswordResetTokenEntity.builder()
                    .tokenId(1).token("valid").build();
            when(tokenRepository.findValidToken(eq("valid"), any(OffsetDateTime.class)))
                    .thenReturn(Optional.of(tokenEntity));

            assertThat(passwordResetService.isTokenValid("valid")).isTrue();
        }

        @Test
        void shouldReturnFalseForInvalidToken() {
            when(tokenRepository.findValidToken(eq("invalid"), any(OffsetDateTime.class)))
                    .thenReturn(Optional.empty());

            assertThat(passwordResetService.isTokenValid("invalid")).isFalse();
        }
    }

    // ---------------------- cleanupExpiredTokens ----------------------
    @Nested
    @DisplayName("cleanupExpiredTokens")
    class CleanupExpiredTokens {

        @Test
        void shouldDeleteExpiredTokens() {
            when(tokenRepository.deleteExpiredTokens(any(OffsetDateTime.class))).thenReturn(5);

            passwordResetService.cleanupExpiredTokens();

            verify(tokenRepository).deleteExpiredTokens(any(OffsetDateTime.class));
        }
    }
}
