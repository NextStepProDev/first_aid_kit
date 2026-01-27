package com.firstaidkit.integration.base;

import com.firstaidkit.config.NoSecurityConfig;
import com.firstaidkit.config.TestCacheConfig;
import com.firstaidkit.config.TestFlywayRunner;
import com.firstaidkit.config.TestSecurityConfig;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.repository.DrugRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import({NoSecurityConfig.class, TestCacheConfig.class, TestFlywayRunner.class, TestSecurityConfig.class})
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer postgresContainer = new PostgreSQLContainer("postgres:17.4")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    @Autowired
    protected DrugRepository drugRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected CacheManager cacheManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    /**
     * Returns the test user (user_id = 1) created by Flyway migration.
     * This user is used as owner for test drug entities.
     */
    protected UserEntity getTestUser() {
        return userRepository.findByUserId(TestSecurityConfig.TEST_USER_ID)
                .orElseThrow(() -> new IllegalStateException("Test user not found. Ensure Flyway migrations ran."));
    }

    @BeforeEach
    void resetData() {
        drugRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }
}