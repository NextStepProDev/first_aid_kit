package com.firstaid.infrastructure.config;

import com.firstaid.infrastructure.database.entity.RoleEntity;
import com.firstaid.infrastructure.database.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
    }

    private void initializeRoles() {
        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("USER");
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByRole(roleName).isEmpty()) {
            RoleEntity role = RoleEntity.builder()
                    .role(roleName)
                    .build();
            roleRepository.save(role);
            log.info("Created missing role: {}", roleName);
        }
    }
}
