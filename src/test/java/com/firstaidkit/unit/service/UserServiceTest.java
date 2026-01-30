package com.firstaidkit.unit.service;

import com.firstaidkit.domain.exception.ResourceNotFoundException;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    // ---------------------- findById ----------------------
    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        void shouldReturnOptionalWithUser() {
            UserEntity user = UserEntity.builder().userId(1).userName("john").email("john@x.com").build();
            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            Optional<UserEntity> result = userService.findById(1);

            assertThat(result).isPresent();
            assertThat(result.get().getUserName()).isEqualTo("john");
        }

        @Test
        void shouldReturnEmptyOptionalWhenNotFound() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            Optional<UserEntity> result = userService.findById(999);

            assertThat(result).isEmpty();
        }
    }

    // ---------------------- getUserOrThrow ----------------------
    @Nested
    @DisplayName("getUserOrThrow")
    class GetUserOrThrow {

        @Test
        void shouldReturnUserWhenExists() {
            UserEntity user = UserEntity.builder().userId(1).userName("john").email("john@x.com").build();
            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            UserEntity result = userService.getUserOrThrow(1);

            assertThat(result.getUserId()).isEqualTo(1);
            assertThat(result.getUserName()).isEqualTo("john");
        }

        @Test
        void shouldThrowResourceNotFoundWhenMissing() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserOrThrow(999))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User with ID 999");
        }
    }
}
