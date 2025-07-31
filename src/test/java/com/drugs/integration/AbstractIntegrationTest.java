package com.drugs.integration;

import com.drugs.config.NoSecurityConfig;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(NoSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    @Container
    @SuppressWarnings({"unused", "resource"})
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17.4")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    @Autowired
    protected DrugsRepository drugsRepository;
    @Autowired
    protected CacheManager cacheManager;

    @BeforeEach
    void resetData() {
        drugsRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}