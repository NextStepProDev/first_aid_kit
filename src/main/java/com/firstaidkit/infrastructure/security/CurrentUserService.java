package com.firstaidkit.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to retrieve the currently authenticated user's information.
 * Used for multi-tenancy filtering in the application.
 */
@Service
@Slf4j
public class CurrentUserService {

    /**
     * Gets the current authenticated user's ID.
     *
     * @return the user ID of the currently authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    public Integer getCurrentUserId() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUserId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Gets the current authenticated user's username.
     *
     * @return the username of the currently authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    public String getCurrentUsername() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUsername)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Gets the current authenticated user's email.
     *
     * @return the email of the currently authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    public String getCurrentUserEmail() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getEmail)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    /**
     * Gets the current authenticated user's details.
     *
     * @return Optional containing CustomUserDetails if authenticated, empty otherwise
     */
    public Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.debug("No authentication found in security context");
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            return Optional.of(customUserDetails);
        }

        log.debug("Principal is not of type CustomUserDetails: {}", principal.getClass().getName());
        return Optional.empty();
    }

    /**
     * Checks if there is a currently authenticated user.
     *
     * @return true if a user is authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return getCurrentUserDetails().isPresent();
    }
}
