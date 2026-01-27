package com.firstaidkit.infrastructure.security;

import com.firstaidkit.infrastructure.database.entity.RoleEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.RoleRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
@Profile("!test")
public class AdminSecurityValidator implements ApplicationRunner {

    @Value("${app.admin.email}")
    private String adminEmail;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        RoleEntity adminRole = roleRepository.findByRole("ADMIN").orElse(null);
        if (adminRole == null) {
            return;
        }

        grantAdminRoleToConfiguredUser(adminRole);
        removeAdminRoleFromUnauthorizedUsers(adminRole);
    }

    private void grantAdminRoleToConfiguredUser(RoleEntity adminRole) {
        UserEntity adminUser = userRepository.findByEmail(adminEmail);
        if (adminUser != null && adminUser.getRole() != null && !adminUser.getRole().contains(adminRole)) {
            adminUser.getRole().add(adminRole);
            userRepository.save(adminUser);
            log.info("Granted ADMIN role to configured admin user: {}", adminEmail);
        }
    }

    private void removeAdminRoleFromUnauthorizedUsers(RoleEntity adminRole) {
        List<UserEntity> unauthorizedAdmins = userRepository.findByRoleExcludingEmail("ADMIN", adminEmail);

        for (UserEntity user : unauthorizedAdmins) {
            if (user.getRole() != null) {
                user.getRole().remove(adminRole);
                userRepository.save(user);
                log.warn("Removed ADMIN role from unauthorized user: {}", user.getEmail());
            }
        }

        if (!unauthorizedAdmins.isEmpty()) {
            log.info("Admin security validation complete. Removed ADMIN role from {} unauthorized user(s)", unauthorizedAdmins.size());
        }
    }
}
