package com.firstaidkit.unit;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class PostgresContainerSanityTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17.4")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void shouldStartPostgresContainer() {
        assertThat(postgres.isRunning()).isTrue();
        System.out.println("JDBC URL: " + postgres.getJdbcUrl());
        System.out.println("Username: " + postgres.getUsername());
        System.out.println("Password: " + postgres.getPassword());
    }
}