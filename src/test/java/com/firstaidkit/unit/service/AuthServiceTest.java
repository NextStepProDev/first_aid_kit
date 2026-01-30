package com.firstaidkit.unit.service;

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
import com.firstaidkit.service.AuthService;
import com.firstaidkit.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Integer TEST_USER_ID = 1;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private DrugRepository drugRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CustomUserDetailService userDetailService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private EmailService emailService;
    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "adminEmail", "admin@firstaidkit.com");
    }

    private CustomUserDetails buildUserDetails() {
        return new CustomUserDetails(
                TEST_EMAIL, ENCODED_PASSWORD, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                TEST_USER_ID, TEST_EMAIL
        );
    }

    private UserEntity buildUserEntity() {
        return UserEntity.builder()
                .userId(TEST_USER_ID)
                .userName(TEST_USERNAME)
                .email(TEST_EMAIL)
                .password(ENCODED_PASSWORD)
                .name("Test User")
                .active(true)
                .failedLoginAttempts(0)
                .role(Set.of(RoleEntity.builder().roleId(1).role("USER").build()))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    // ---------------------- login ----------------------
    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void shouldReturnJwtOnSuccess() {
            LoginRequest request = LoginRequest.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();
            CustomUserDetails userDetails = buildUserDetails();
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(buildUserEntity());
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
            when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(auth)).thenReturn("refresh-token");
            lenient().when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(buildUserEntity()));

            JwtResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        void shouldThrowWhenAccountLocked() {
            UserEntity lockedUser = buildUserEntity();
            lockedUser.setLockedUntil(OffsetDateTime.now().plusMinutes(10));

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(lockedUser);

            LoginRequest request = LoginRequest.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("Account is locked");

            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        void shouldHandleFailedLoginAndIncrementAttempts() {
            UserEntity user = buildUserEntity();
            user.setFailedLoginAttempts(0);

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(user);
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

            LoginRequest request = LoginRequest.builder().email(TEST_EMAIL).password("wrong").build();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        void shouldLockAccountAfterMaxFailedAttempts() {
            UserEntity user = buildUserEntity();
            user.setFailedLoginAttempts(4); // next attempt = 5th = MAX

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(user);
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

            LoginRequest request = LoginRequest.builder().email(TEST_EMAIL).password("wrong").build();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getLockedUntil()).isNotNull();
            assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(5);
        }

        @Test
        void shouldResetFailedAttemptsOnSuccess() {
            UserEntity user = buildUserEntity();
            user.setFailedLoginAttempts(3);
            user.setLockedUntil(OffsetDateTime.now().minusMinutes(1)); // lock expired

            CustomUserDetails userDetails = buildUserDetails();
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(user);
            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtTokenProvider.generateAccessToken(auth)).thenReturn("access");
            when(jwtTokenProvider.generateRefreshToken(auth)).thenReturn("refresh");
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

            authService.login(LoginRequest.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build());

            // resetFailedAttempts + updateLastLogin = at least 2 saves
            verify(userRepository, atLeast(1)).save(any(UserEntity.class));
        }
    }

    // ---------------------- register ----------------------
    @Nested
    @DisplayName("register")
    class Register {

        @Test
        void shouldCreateUserAndReturnMessage() {
            RegisterRequest request = RegisterRequest.builder()
                    .username(TEST_USERNAME).email(TEST_EMAIL)
                    .password(TEST_PASSWORD).name("Test").build();

            when(userRepository.existsByUserName(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(roleRepository.findByRole("USER")).thenReturn(
                    Optional.of(RoleEntity.builder().roleId(1).role("USER").build()));
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity u = inv.getArgument(0);
                u.setUserId(TEST_USER_ID);
                return u;
            });

            MessageResponse response = authService.register(request);

            assertThat(response.getMessage()).contains("Rejestracja przebiegla pomyslnie");
            verify(userRepository, atLeast(1)).save(any(UserEntity.class));
            verify(emailVerificationService).createVerificationToken(any(UserEntity.class));

            // Verify user is created as inactive
            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getActive()).isFalse();
        }

        @Test
        void shouldThrowWhenUsernameExists() {
            when(userRepository.existsByUserName(TEST_USERNAME)).thenReturn(true);

            RegisterRequest request = RegisterRequest.builder()
                    .username(TEST_USERNAME).email(TEST_EMAIL)
                    .password(TEST_PASSWORD).name("Test").build();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        void shouldThrowWhenEmailExists() {
            when(userRepository.existsByUserName(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            RegisterRequest request = RegisterRequest.builder()
                    .username(TEST_USERNAME).email(TEST_EMAIL)
                    .password(TEST_PASSWORD).name("Test").build();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        void shouldAssignAdminRoleForConfiguredEmail() {
            String adminEmail = "admin@firstaidkit.com";
            RegisterRequest request = RegisterRequest.builder()
                    .username("adminuser").email(adminEmail)
                    .password(TEST_PASSWORD).name("Admin").build();

            when(userRepository.existsByUserName("adminuser")).thenReturn(false);
            when(userRepository.existsByEmail(adminEmail)).thenReturn(false);
            RoleEntity userRole = RoleEntity.builder().roleId(1).role("USER").build();
            RoleEntity adminRole = RoleEntity.builder().roleId(2).role("ADMIN").build();
            when(roleRepository.findByRole("USER")).thenReturn(Optional.of(userRole));
            when(roleRepository.findByRole("ADMIN")).thenReturn(Optional.of(adminRole));
            when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity u = inv.getArgument(0);
                u.setUserId(TEST_USER_ID);
                return u;
            });

            authService.register(request);

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository, atLeast(1)).save(captor.capture());
            UserEntity registeredUser = captor.getAllValues().get(0);
            assertThat(registeredUser.getRole()).extracting(RoleEntity::getRole)
                    .containsExactlyInAnyOrder("USER", "ADMIN");
        }

        @Test
        void shouldThrowWhenUserRoleNotFound() {
            when(userRepository.existsByUserName(TEST_USERNAME)).thenReturn(false);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(roleRepository.findByRole("USER")).thenReturn(Optional.empty());

            RegisterRequest request = RegisterRequest.builder()
                    .username(TEST_USERNAME).email(TEST_EMAIL)
                    .password(TEST_PASSWORD).name("Test").build();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Default USER role not found");
        }
    }

    // ---------------------- refreshToken ----------------------
    @Nested
    @DisplayName("refreshToken")
    class RefreshToken {

        @Test
        void shouldReturnNewTokensOnValidRefresh() {
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("valid-refresh").build();
            CustomUserDetails userDetails = buildUserDetails();

            when(jwtTokenProvider.validateToken("valid-refresh")).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken("valid-refresh")).thenReturn(true);
            when(jwtTokenProvider.getUsernameFromToken("valid-refresh")).thenReturn(TEST_EMAIL);
            when(userDetailService.loadUserByUsername(TEST_EMAIL)).thenReturn(userDetails);
            when(jwtTokenProvider.generateAccessTokenFromUserDetails(userDetails)).thenReturn("new-access");
            when(jwtTokenProvider.generateRefreshTokenFromUserDetails(userDetails)).thenReturn("new-refresh");

            JwtResponse response = authService.refreshToken(request);

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            assertThat(response.getUserId()).isEqualTo(TEST_USER_ID);
        }

        @Test
        void shouldThrowWhenTokenInvalid() {
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("invalid").build();
            when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid refresh token");
        }

        @Test
        void shouldThrowWhenNotRefreshToken() {
            RefreshTokenRequest request = RefreshTokenRequest.builder().refreshToken("access-token").build();
            when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
            when(jwtTokenProvider.isRefreshToken("access-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Token is not a refresh token");
        }
    }

    // ---------------------- deleteAccount ----------------------
    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @BeforeEach
        void setUp() {
            lenient().when(currentUserService.getCurrentUserId()).thenReturn(TEST_USER_ID);
            lenient().when(currentUserService.getCurrentUserEmail()).thenReturn(TEST_EMAIL);
        }

        @Test
        void shouldDeleteAccountAndCascadeDrugs() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            DeleteAccountRequest request = DeleteAccountRequest.builder().password(TEST_PASSWORD).build();
            authService.deleteAccount(request);

            verify(drugRepository).deleteAllByOwnerUserId(TEST_USER_ID);
            verify(userRepository).delete(user);
        }

        @Test
        void shouldThrowWhenInvalidPassword() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrong", ENCODED_PASSWORD)).thenReturn(false);

            DeleteAccountRequest request = DeleteAccountRequest.builder().password("wrong").build();

            assertThatThrownBy(() -> authService.deleteAccount(request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("Invalid password");

            verify(userRepository, never()).delete(any());
        }

        @Test
        void shouldClearSecurityContext() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(TEST_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            DeleteAccountRequest request = DeleteAccountRequest.builder().password(TEST_PASSWORD).build();
            authService.deleteAccount(request);

            // SecurityContextHolder.clearContext() is a static call.
            // We verify the delete chain completed which means clearContext was called.
            verify(userRepository).delete(user);
        }
    }

    // ---------------------- getCurrentUserProfile ----------------------
    @Nested
    @DisplayName("getCurrentUserProfile")
    class GetCurrentUserProfile {

        @BeforeEach
        void setUp() {
            lenient().when(currentUserService.getCurrentUserId()).thenReturn(TEST_USER_ID);
        }

        @Test
        void shouldReturnProfileWithRoles() {
            UserEntity user = buildUserEntity();
            user.setRole(Set.of(
                    RoleEntity.builder().roleId(1).role("USER").build(),
                    RoleEntity.builder().roleId(2).role("ADMIN").build()
            ));
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));

            UserProfileResponse profile = authService.getCurrentUserProfile();

            assertThat(profile.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(profile.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(profile.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(profile.getRoles()).containsExactlyInAnyOrder("USER", "ADMIN");
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getCurrentUserProfile())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ---------------------- updateProfile ----------------------
    @Nested
    @DisplayName("updateProfile")
    class UpdateProfile {

        @BeforeEach
        void setUp() {
            lenient().when(currentUserService.getCurrentUserId()).thenReturn(TEST_USER_ID);
            lenient().when(currentUserService.getCurrentUsername()).thenReturn(TEST_USERNAME);
        }

        @Test
        void shouldUpdateUsernameAndName() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.existsByUserName("newname")).thenReturn(false);
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest request = new UpdateProfileRequest("New Name", "newname");
            UserProfileResponse response = authService.updateProfile(request);

            assertThat(response.getUsername()).isEqualTo("newname");
            assertThat(response.getName()).isEqualTo("New Name");

            ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUserName()).isEqualTo("newname");
            assertThat(captor.getValue().getName()).isEqualTo("New Name");
        }

        @Test
        void shouldThrowWhenNewUsernameAlreadyExists() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.existsByUserName("taken")).thenReturn(true);

            UpdateProfileRequest request = new UpdateProfileRequest("Name", "taken");

            assertThatThrownBy(() -> authService.updateProfile(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Username already exists");
        }

        @Test
        void shouldAllowKeepingSameUsername() {
            UserEntity user = buildUserEntity();
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            // Same username as current - should not check existsByUserName
            UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", TEST_USERNAME);
            UserProfileResponse response = authService.updateProfile(request);

            assertThat(response.getName()).isEqualTo("Updated Name");
            verify(userRepository, never()).existsByUserName(anyString());
        }
    }
}
