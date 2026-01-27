package com.firstaidkit.integration.cache;

import com.firstaidkit.infrastructure.database.repository.DrugRepository;
import com.firstaidkit.infrastructure.security.CurrentUserService;
import com.firstaidkit.integration.base.AbstractIntegrationTest;
import com.firstaidkit.service.DrugService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.*;

class DrugCacheIntegrationTest  extends AbstractIntegrationTest {

    @Autowired
    private DrugService drugService;

    @MockitoSpyBean
    private DrugRepository drugRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Test
    void shouldIsolateCacheBetweenUsers() {
        // GIVEN: Użytkownik 1
        Integer user1Id = 1;
        when(currentUserService.getCurrentUserId()).thenReturn(user1Id);
        
        // Pierwsze wywołanie dla User 1 - idzie do bazy
        drugService.getDrugStatistics();
        
        // Drugie wywołanie dla User 1 - powinno wziąć z CACHE
        drugService.getDrugStatistics();

        // THEN: Dla User 1 baza powinna być uderzona tylko RAZ
        verify(drugRepository, times(1)).countByOwnerUserId(user1Id);

        // WHEN: Przełączamy się na Użytkownika 2
        Integer user2Id = 2;
        when(currentUserService.getCurrentUserId()).thenReturn(user2Id);

        // Wywołanie dla User 2
        drugService.getDrugStatistics();

        // THEN: Baza danych musi być uderzona ponownie, bo User 2 nie może użyć cache Usera 1
        verify(drugRepository, times(1)).countByOwnerUserId(user2Id);
    }
}