package com.firstaidkit.infrastructure.bootstrap;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class FlywayInitializer {

    private final DataSource dataSource;

    @Value("${spring.profiles.active:}")
    private String profile;

    @PostConstruct
    public void runMigrations() {
        if ("local".equals(profile)) {
            log.info("Clearing database for local profile...");
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .cleanDisabled(false)
                    .load();
            flyway.clean();
            flyway.migrate();
            log.info("Database reset and migrations completed.");
        } else {
            log.info("Running Flyway migrations...");
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            log.info("Flyway migrations completed.");
        }
    }
}