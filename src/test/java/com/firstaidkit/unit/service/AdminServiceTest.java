package com.firstaidkit.unit.service;

import com.firstaidkit.controller.dto.admin.BroadcastEmailRequest;
import com.firstaidkit.controller.dto.admin.UserResponse;
import com.firstaidkit.domain.exception.InvalidPasswordException;
import com.firstaidkit.domain.exception.UserNotFoundException;
import com.firstaidkit.infrastructure.database.entity.RoleEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.DrugRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.infrastructure.email.EmailService;
import com.firstaidkit.infrastructure.security.CurrentUserService;
import com.firstaidkit.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final Integer ADMIN_ID = 1;
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "adminPass";
    private static final String ENCODED_PASSWORD = "encodedAdminPass";

    @Mock
    private UserRepository userRepository;
    @Mock
    private DrugRepository drugRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(ADMIN_ID);
        lenient().when(currentUserService.getCurrentUserEmail()).thenReturn(ADMIN_EMAIL);
    }

    // ---------------------- getAllUsers ----------------------
    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        void shouldReturnPageOfUserResponses() {
            Pageable pageable = PageRequest.of(0, 10);
            UserEntity user = UserEntity.builder()
                    .userId(2)
                    .userName("john")
                    .email("john@example.com")
                    .name("John")
                    .active(true)
                    .role(Set.of(RoleEntity.builder().roleId(1).role("USER").build()))
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

            Page<UserResponse> result = adminService.getAllUsers(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).username()).isEqualTo("john");
            assertThat(result.getContent().get(0).email()).isEqualTo("john@example.com");
        }

        @Test
        void shouldMapRolesCorrectly() {
            Pageable pageable = PageRequest.of(0, 10);
            UserEntity user = UserEntity.builder()
                    .userId(2)
                    .userName("admin2")
                    .email("admin2@example.com")
                    .name("Admin Two")
                    .active(true)
                    .role(Set.of(
                            RoleEntity.builder().roleId(1).role("USER").build(),
                            RoleEntity.builder().roleId(2).role("ADMIN").build()
                    ))
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

            Page<UserResponse> result = adminService.getAllUsers(pageable);

            assertThat(result.getContent().get(0).roles()).containsExactlyInAnyOrder("USER", "ADMIN");
        }

        @Test
        void shouldHandleNullRolesGracefully() {
            Pageable pageable = PageRequest.of(0, 10);
            UserEntity user = UserEntity.builder()
                    .userId(3)
                    .userName("norole")
                    .email("norole@example.com")
                    .name("No Role")
                    .active(true)
                    .role(null)
                    .createdAt(OffsetDateTime.now())
                    .build();
            when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 1));

            Page<UserResponse> result = adminService.getAllUsers(pageable);

            assertThat(result.getContent().get(0).roles()).isEmpty();
        }
    }

    // ---------------------- deleteUser ----------------------
    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        private UserEntity adminEntity;

        @BeforeEach
        void setUp() {
            adminEntity = UserEntity.builder()
                    .userId(ADMIN_ID)
                    .email(ADMIN_EMAIL)
                    .password(ENCODED_PASSWORD)
                    .build();
        }

        @Test
        void shouldDeleteUserAndCascadeDrugs() {
            Integer targetUserId = 5;
            UserEntity targetUser = UserEntity.builder()
                    .userId(targetUserId)
                    .email("target@example.com")
                    .build();

            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
            when(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            adminService.deleteUser(targetUserId, ADMIN_PASSWORD);

            verify(drugRepository).deleteAllByOwnerUserId(targetUserId);
            verify(userRepository).delete(targetUser);
        }

        @Test
        void shouldThrowWhenInvalidPassword() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
            when(passwordEncoder.matches("wrongPass", ENCODED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> adminService.deleteUser(5, "wrongPass"))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("Invalid password");

            verify(userRepository, never()).delete(any());
        }

        @Test
        void shouldThrowWhenDeletingSelf() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
            when(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

            assertThatThrownBy(() -> adminService.deleteUser(ADMIN_ID, ADMIN_PASSWORD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot delete your own account");

            verify(userRepository, never()).delete(any());
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(adminEntity));
            when(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser(999, ADMIN_PASSWORD))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        void shouldThrowWhenAdminNotFound() {
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.deleteUser(5, ADMIN_PASSWORD))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Admin user not found");
        }
    }

    // ---------------------- broadcastEmail ----------------------
    @Nested
    @DisplayName("broadcastEmail")
    class BroadcastEmail {

        @Test
        void shouldSendToActiveUsersOnly() {
            UserEntity active1 = UserEntity.builder().userId(1).email("a@x.com").active(true).build();
            UserEntity active2 = UserEntity.builder().userId(2).email("b@x.com").active(true).build();
            UserEntity inactive = UserEntity.builder().userId(3).email("c@x.com").active(false).build();
            when(userRepository.findAll()).thenReturn(List.of(active1, active2, inactive));

            BroadcastEmailRequest request = new BroadcastEmailRequest("Subject", "Body");
            int count = adminService.broadcastEmail(request);

            assertThat(count).isEqualTo(2);
            verify(emailService).sendEmailAsync("a@x.com", "Subject", "Body");
            verify(emailService).sendEmailAsync("b@x.com", "Subject", "Body");
            verify(emailService, never()).sendEmailAsync(eq("c@x.com"), anyString(), anyString());
        }

        @Test
        void shouldReturnZeroWhenNoActiveUsers() {
            UserEntity inactive = UserEntity.builder().userId(1).email("c@x.com").active(false).build();
            when(userRepository.findAll()).thenReturn(List.of(inactive));

            BroadcastEmailRequest request = new BroadcastEmailRequest("Subject", "Body");
            int count = adminService.broadcastEmail(request);

            assertThat(count).isEqualTo(0);
            verify(emailService, never()).sendEmailAsync(anyString(), anyString(), anyString());
        }

        @Test
        void shouldCountCorrectly() {
            List<UserEntity> users = List.of(
                    UserEntity.builder().userId(1).email("a@x.com").active(true).build(),
                    UserEntity.builder().userId(2).email("b@x.com").active(true).build(),
                    UserEntity.builder().userId(3).email("c@x.com").active(true).build()
            );
            when(userRepository.findAll()).thenReturn(users);

            BroadcastEmailRequest request = new BroadcastEmailRequest("Hi", "Hello");
            int count = adminService.broadcastEmail(request);

            assertThat(count).isEqualTo(3);
            verify(emailService, times(3)).sendEmailAsync(anyString(), eq("Hi"), eq("Hello"));
        }
    }

    // ---------------------- exportEmailsCsv ----------------------
    @Nested
    @DisplayName("exportEmailsCsv")
    class ExportEmailsCsv {

        @Test
        void shouldGenerateValidCsvWithHeader() {
            OffsetDateTime createdAt = OffsetDateTime.parse("2025-01-15T10:00:00Z");
            UserEntity user = UserEntity.builder()
                    .userId(1).email("test@x.com").name("Test User").userName("testuser")
                    .active(true).createdAt(createdAt).build();
            when(userRepository.findAll()).thenReturn(List.of(user));

            String csv = adminService.exportEmailsCsv();

            assertThat(csv).startsWith("email,name,username,active,created_at\n");
            assertThat(csv).contains("test@x.com");
            assertThat(csv).contains("Test User");
            assertThat(csv).contains("testuser");
            assertThat(csv).contains("true");
        }

        @Test
        void shouldEscapeCommasAndQuotes() {
            UserEntity user = UserEntity.builder()
                    .userId(1).email("test@x.com").name("Last, First")
                    .userName("user\"name").active(true).createdAt(OffsetDateTime.now()).build();
            when(userRepository.findAll()).thenReturn(List.of(user));

            String csv = adminService.exportEmailsCsv();

            // Name with comma should be quoted
            assertThat(csv).contains("\"Last, First\"");
            // Username with quote should be escaped
            assertThat(csv).contains("\"user\"\"name\"");
        }

        @Test
        void shouldHandleNullNameFields() {
            UserEntity user = UserEntity.builder()
                    .userId(1).email("test@x.com").name(null).userName("testuser")
                    .active(true).createdAt(null).build();
            when(userRepository.findAll()).thenReturn(List.of(user));

            String csv = adminService.exportEmailsCsv();

            // Should not throw and should contain the email
            assertThat(csv).contains("test@x.com");
            // Null name mapped to empty string
            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(2); // header + 1 data row
        }
    }
}
