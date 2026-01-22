package com.firstaid.controller.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer userId;
    private String username;
    private String email;

    public static JwtResponse of(String accessToken, String refreshToken, Integer userId, String username, String email) {
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(userId)
                .username(username)
                .email(email)
                .build();
    }
}
