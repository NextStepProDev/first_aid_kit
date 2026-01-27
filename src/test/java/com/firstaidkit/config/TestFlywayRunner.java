package com.firstaidkit.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * Uruchamia migracje Flyway w testach niezależnie od autokonfiguracji Flyway.
 * Nie wymaga istnienia beana Flyway. proxyBeanMethods = false żeby uniknąć CGLIB.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestFlywayRunner {

    @Bean
    public ApplicationListener<ApplicationReadyEvent> runMigrations(ObjectProvider<DataSource> dsProvider) {
        return ev -> dsProvider.ifAvailable(ds -> {
            // Utwórz Flyway ręcznie i wykonaj migracje
            Flyway.configure()
                    .dataSource(ds)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
        });
    }
}