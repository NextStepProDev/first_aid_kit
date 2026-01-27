package com.firstaidkit.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import java.util.Collections;

/**
 * Test-safe security config. Active only for 'test' profile.
 * Returns an allow-all SecurityFilterChain without depending on HttpSecurity.
 */
@Configuration
@Profile("test")
public class NoSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain() {
        return new SecurityFilterChain() {
            @Override
            public boolean matches(HttpServletRequest request) {
                return AnyRequestMatcher.INSTANCE.matches(request);
            }

            @Override
            public java.util.List<Filter> getFilters() {
                return Collections.emptyList();
            }
        };
    }
}