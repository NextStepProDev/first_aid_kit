package com.firstaid.infrastructure.security;

import com.firstaid.infrastructure.database.entity.RoleEntity;
import com.firstaid.infrastructure.database.entity.UserEntity;
import com.firstaid.infrastructure.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        UserEntity user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        List<SimpleGrantedAuthority> authorities = getUserAuthority(user.getRole());
        return buildUserForAuthentication(user, authorities);
    }

    private List<SimpleGrantedAuthority> getUserAuthority(Set<RoleEntity> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole()))
                .distinct()
                .toList();
    }

    private CustomUserDetails buildUserForAuthentication(UserEntity user, List<SimpleGrantedAuthority> authorities) {
        return new CustomUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.getActive(),
                authorities,
                user.getUserId(),
                user.getEmail()
        );
    }
}
