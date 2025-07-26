package com.drugs.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class ZajavkaUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = userRepository.findByUserName(username);
        List<SimpleGrantedAuthority> authorities =  getUserAuthority(user.getRole());
        return buildUserForAuthentication(user, authorities);

    }

    private List<SimpleGrantedAuthority> getUserAuthority(Set<RoleEntity> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRole()))
                .distinct()
                .toList();
    }

    private UserDetails buildUserForAuthentication(UserEntity user, List<SimpleGrantedAuthority> authorities) {
        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                user.getActive(),
                true,
                true,
                true,
                authorities
        );
    }
//    $2a$12$VKSNEst66WnLr68DfDBq9OdWysgHcBKWBQrPkheiNuUsO.iRuWJ6e
}
