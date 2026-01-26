package com.firstaid.infrastructure.bootstrap;

import com.firstaid.infrastructure.database.entity.RoleEntity;
import com.firstaid.infrastructure.database.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DataInitializer implements SmartApplicationListener {

    private final RoleRepository roleRepository;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;  // Runs after BootstrapApplicationComponent
    }

    @Override
    public boolean supportsEventType(@NonNull Class<? extends ApplicationEvent> eventType) {
        return ApplicationReadyEvent.class.isAssignableFrom(eventType);
    }

    @Override
    @Transactional
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
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