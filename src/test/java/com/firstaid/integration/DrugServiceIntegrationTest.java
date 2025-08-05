package com.firstaid.integration;

import com.firstaid.service.DrugService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;

@SpringBootTest
class DrugServiceIntegrationTest {

    @MockitoSpyBean
    @SuppressWarnings("unused")
    private DrugService drugService;

    /*
    Sorry guys, couldn't test this in a unit test properly – method calls another method in the same service. SpyBean
    to the rescue!
     */
    @Test
    @DisplayName("Should delegate to sendExpiryAlertEmails() with next month's year and month")
    void shouldDelegateToSendExpiryAlertEmails_withNextMonthDate() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        int expectedYear = now.getMonthValue() == 12 ? now.getYear() + 1 : now.getYear();
        int expectedMonth = now.getMonthValue() == 12 ? 1 : now.getMonthValue() + 1;

        // when
        drugService.sendDefaultExpiryAlertEmails();

        // then
        verify(drugService).sendExpiryAlertEmails(expectedYear, expectedMonth);
    }
}