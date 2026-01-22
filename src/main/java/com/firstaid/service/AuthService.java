package com.firstaid.service;

import com.firstaid.controller.dto.auth.JwtResponse;
import com.firstaid.controller.dto.auth.LoginRequest;
import com.firstaid.controller.dto.auth.RefreshTokenRequest;
import com.firstaid.controller.dto.auth.RegisterRequest;
import com.firstaid.infrastructure.database.entity.RoleEntity;
import com.firstaid.infrastructure.database.entity.UserEntity;
import com.firstaid.infrastructure.database.repository.RoleRepository;
import com.firstaid.infrastructure.database.repository.UserRepository;
import com.firstaid.infrastructure.security.CustomUserDetails;
import com.firstaid.infrastructure.security.CustomUserDetailService;
import com.firstaid.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailService userDetailService;

    public JwtResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            log.info("User logged in successfully: {}", request.getEmail());

            return JwtResponse.of(
                    accessToken,
                    refreshToken,
                    userDetails.getUserId(),
                    userDetails.getUsername(),
                    userDetails.getEmail()
            );
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public JwtResponse register(RegisterRequest request) {
        if (userRepository.existsByUserName(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        RoleEntity userRole = roleRepository.findByRole("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));

        UserEntity user = UserEntity.builder()
                .userName(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .active(true)
                .role(Set.of(userRole))
                .build();

        UserEntity savedUser = userRepository.save(user);
        log.info("New user registered: {}", savedUser.getUserName());

        // Auto-login after registration
        return login(LoginRequest.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .build());
    }

    public JwtResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Token is not a refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        CustomUserDetails userDetails = (CustomUserDetails) userDetailService.loadUserByUsername(username);

        String newAccessToken = jwtTokenProvider.generateAccessTokenFromUserDetails(userDetails);

        log.info("Token refreshed for user: {}", username);

        return JwtResponse.of(
                newAccessToken,
                refreshToken, // Return the same refresh token
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getEmail()
        );
    }
}
