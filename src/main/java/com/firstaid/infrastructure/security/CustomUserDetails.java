package com.firstaid.infrastructure.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Custom UserDetails implementation that includes the userId for multi-tenancy support.
 */
@Getter
public class CustomUserDetails extends User {

    private final Integer userId;
    private final String email;

    public CustomUserDetails(String username,
                             String password,
                             boolean enabled,
                             Collection<? extends GrantedAuthority> authorities,
                             Integer userId,
                             String email) {
        super(username, password, enabled, true, true, true, authorities);
        this.userId = userId;
        this.email = email;
    }

    public CustomUserDetails(String username,
                             String password,
                             Collection<? extends GrantedAuthority> authorities,
                             Integer userId,
                             String email) {
        this(username, password, true, authorities, userId, email);
    }
}
