package com.firstaidkit.integration.cache;

import com.firstaidkit.controller.dto.drug.DrugStatistics;
import com.firstaidkit.infrastructure.database.entity.DrugEntity;
import com.firstaidkit.infrastructure.database.entity.DrugFormEntity;
import com.firstaidkit.infrastructure.database.repository.DrugFormRepository;
import com.firstaidkit.infrastructure.email.EmailService;
import com.firstaidkit.infrastructure.util.DateUtils;
import com.firstaidkit.integration.base.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that cache is properly evicted when sending expiry alerts.
 * This test exists to catch bugs related to Spring AOP proxy behavior with @CacheEvict.
 * When a method with @CacheEvict calls another method in the same class internally,
 * the cache eviction annotation on the inner method is NOT executed (because internal
 * calls don't go through the Spring proxy). This test ensures that the outer method
 * has the proper @CacheEvict annotation.
 */
class CacheEvictionIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DrugFormRepository drugFormRepository;

    @MockitoBean
    @SuppressWarnings("unused")
    private EmailService emailService;

    private String getUrl(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    @DisplayName("Statistics cache should be evicted after sending default expiry alerts")
    void shouldEvictStatisticsCacheAfterSendingDefaultExpiryAlerts() {
        // given: a drug expiring within the next month (will be picked up by default alert)
        DrugFormEntity pillsForm = drugFormRepository.findByNameIgnoreCase("PILLS")
                .orElseThrow(() -> new IllegalStateException("PILLS form not found"));

        OffsetDateTime expirationDate =
                DateUtils.buildExpirationDate(OffsetDateTime.now().getYear(), OffsetDateTime.now().getMonthValue());
        DrugEntity expiringDrug = DrugEntity.builder()
                .drugName("Expiring Drug")
                .drugDescription("Test drug for cache eviction")
                .drugForm(pillsForm)
                .owner(getTestUser())
                .expirationDate(expirationDate)
                .alertSent(false)
                .build();
        drugRepository.save(expiringDrug);

        // when: first call to statistics - cache gets populated
        ResponseEntity<DrugStatistics> firstStatsResponse = restTemplate.getForEntity(
                getUrl("/api/drugs/statistics"), DrugStatistics.class);

        assertThat(firstStatsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DrugStatistics firstStats = firstStatsResponse.getBody();
        assertThat(firstStats).isNotNull();
        assertThat(firstStats.getAlertSentCount()).isEqualTo(0L);

        // when: send alerts via POST /api/email/alert (uses sendDefaultExpiryAlertEmailsForCurrentUser)
        ResponseEntity<String> alertResponse = restTemplate.postForEntity(
                getUrl("/api/email/alert"), null, String.class);

        assertThat(alertResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // then: second call to statistics should reflect the updated alertSentCount
        // If cache was NOT evicted, we would still get alertSentCount = 0 (stale cached value)
        // If cache WAS evicted, we should get alertSentCount = 1 (fresh value from DB)
        ResponseEntity<DrugStatistics> secondStatsResponse = restTemplate.getForEntity(
                getUrl("/api/drugs/statistics"), DrugStatistics.class);

        assertThat(secondStatsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DrugStatistics secondStats = secondStatsResponse.getBody();
        assertThat(secondStats).isNotNull();
        assertThat(secondStats.getAlertSentCount())
                .as("alertSentCount should be updated after sending alerts (cache should be evicted)")
                .isEqualTo(1L);
    }

    @Test
    @DisplayName("Statistics cache should NOT return stale data - regression test for Spring AOP proxy issue")
    void shouldNotReturnStaleCacheDataAfterAlerts() {
        // This test verifies the fix for a bug where @CacheEvict on internal method calls
        // was not being executed due to Spring AOP proxy behavior.
        //
        // The bug: sendDefaultExpiryAlertEmailsForCurrentUser() called sendExpiryAlertEmailsForCurrentUser()
        // internally. The inner method had @CacheEvict, but internal calls don't go through Spring's proxy,
        // so the cache was never evicted. Users saw alertSentCount=0 even after alerts were sent.

        // given: multiple drugs expiring soon
        DrugFormEntity pillsForm = drugFormRepository.findByNameIgnoreCase("PILLS")
                .orElseThrow(() -> new IllegalStateException("PILLS form not found"));

        OffsetDateTime expirationDate =
                DateUtils.buildExpirationDate(OffsetDateTime.now().getYear(), OffsetDateTime.now().getMonthValue());

        for (int i = 1; i <= 3; i++) {
            DrugEntity drug = DrugEntity.builder()
                    .drugName("Expiring Drug " + i)
                    .drugDescription("Test drug " + i)
                    .drugForm(pillsForm)
                    .owner(getTestUser())
                    .expirationDate(expirationDate)
                    .alertSent(false)
                    .build();
            drugRepository.save(drug);
        }

        // when: populate cache
        ResponseEntity<DrugStatistics> cachedResponse = restTemplate.getForEntity(
                getUrl("/api/drugs/statistics"), DrugStatistics.class);
        assertThat(cachedResponse.getBody()).isNotNull();
        assertThat(cachedResponse.getBody().getAlertSentCount()).isEqualTo(0L);
        assertThat(cachedResponse.getBody().getTotalDrugs()).isEqualTo(3L);

        // when: trigger alert sending
        restTemplate.postForEntity(getUrl("/api/email/alert"), null, String.class);

        // then: fresh data should be returned, not stale cache
        ResponseEntity<DrugStatistics> freshResponse = restTemplate.getForEntity(
                getUrl("/api/drugs/statistics"), DrugStatistics.class);

        assertThat(freshResponse.getBody()).isNotNull();
        assertThat(freshResponse.getBody().getAlertSentCount())
                .as("Cache should be evicted - alertSentCount must reflect all 3 alerts sent")
                .isEqualTo(3L);
    }
}
