package com.firstaidkit.config;

import com.firstaidkit.infrastructure.security.CurrentUserService;
import com.firstaidkit.infrastructure.security.CustomUserDetails;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

/**
 * Test configuration that provides a mock CurrentUserService
 * for integration tests that bypass security but need user context.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    public static final Integer TEST_USER_ID = 1;
    public static final String TEST_USERNAME = "testuser";
    public static final String TEST_EMAIL = "test@example.com";

    @Bean
    @Primary
    public CurrentUserService testCurrentUserService() {
        CurrentUserService mock = Mockito.mock(CurrentUserService.class);

        CustomUserDetails testUser = new CustomUserDetails(
                TEST_USERNAME,
                "password",
                true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                TEST_USER_ID,
                TEST_EMAIL
        );

        Mockito.when(mock.getCurrentUserId()).thenReturn(TEST_USER_ID);
        Mockito.when(mock.getCurrentUsername()).thenReturn(TEST_USERNAME);
        Mockito.when(mock.getCurrentUserEmail()).thenReturn(TEST_EMAIL);
        Mockito.when(mock.getCurrentUserDetails()).thenReturn(Optional.of(testUser));
        Mockito.when(mock.isAuthenticated()).thenReturn(true);

        return mock;
    }
}
