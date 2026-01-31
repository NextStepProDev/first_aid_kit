package com.firstaidkit.controller.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Integer userId;
    private String username;
    private String email;
    private String name;
    private Set<String> roles;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastLogin;
    private Boolean alertsEnabled;
}
