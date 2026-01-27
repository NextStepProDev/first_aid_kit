package com.firstaidkit.controller.dto.admin;

import java.time.OffsetDateTime;
import java.util.Set;

public record UserResponse(
        Integer userId,
        String username,
        String email,
        String name,
        Boolean active,
        Set<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime lastLogin
) {}
