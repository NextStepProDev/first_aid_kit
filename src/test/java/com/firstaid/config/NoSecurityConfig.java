package com.firstaid.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
    public class NoSecurityConfig {

        /**
         * This configuration disables security for tests, allowing all requests to pass through without authentication.
         * It is useful for testing purposes when you want to avoid dealing with security constraints.
         *
         * @param http the HttpSecurity object to configure
         * @return a SecurityFilterChain that allows all requests
         * @throws Exception if an error occurs during configuration
         */

        @Bean
        @SuppressWarnings("unused")
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/**")
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable)
                    .build();
        }
    }