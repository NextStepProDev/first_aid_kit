package com.firstaidkit.unit.service;

import com.firstaidkit.controller.dto.drug.DrugCreateRequest;
import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.controller.dto.drug.DrugResponse;
import com.firstaidkit.controller.dto.drug.DrugStatistics;
import com.firstaidkit.domain.exception.DrugNotFoundException;
import com.firstaidkit.domain.exception.EmailSendingException;
import com.firstaidkit.infrastructure.database.entity.DrugEntity;
import com.firstaidkit.infrastructure.database.entity.DrugFormEntity;
import com.firstaidkit.infrastructure.database.entity.UserEntity;
import com.firstaidkit.infrastructure.database.mapper.DrugMapper;
import com.firstaidkit.infrastructure.database.repository.DrugRepository;
import com.firstaidkit.infrastructure.database.repository.UserRepository;
import com.firstaidkit.infrastructure.email.EmailService;
import com.firstaidkit.infrastructure.security.CurrentUserService;
import com.firstaidkit.infrastructure.util.DateUtils;
import com.firstaidkit.service.DrugFormService;
import com.firstaidkit.service.DrugService;
import com.firstaidkit.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrugServiceTest {

    private static final Integer TEST_USER_ID = 1;
    private static final String TEST_USER_EMAIL = "test@example.com";
    static int YEAR_NOW_PLUS_1 = OffsetDateTime.now().plusYears(1).getYear();
    @Mock
    private DrugRepository drugRepository;
    @Mock
    private DrugFormService drugFormService;
    @Mock
    private DrugMapper drugMapper;
    @Mock
    private EmailService emailService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @InjectMocks
    private DrugService drugService;

    @BeforeEach
    void setUp() {
        // Set up default user context for all tests
        lenient().when(currentUserService.getCurrentUserId()).thenReturn(TEST_USER_ID);
        lenient().when(currentUserService.getCurrentUserEmail()).thenReturn(TEST_USER_EMAIL);
        lenient().when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(UserEntity.builder().userId(TEST_USER_ID).email(TEST_USER_EMAIL).build()));
    }

    // ---------------------- getDrugById ----------------------
    @Nested
    @DisplayName("getDrugById")
    class GetDrugById {
        @Test
        void shouldThrowWhenNotFound() {
            when(drugRepository.findByDrugIdAndOwnerUserId(999, TEST_USER_ID)).thenReturn(Optional.empty());
            assertThrows(DrugNotFoundException.class, () -> drugService.getDrugById(999));
            verify(drugRepository).findByDrugIdAndOwnerUserId(999, TEST_USER_ID);
        }

        @Test
        void shouldReturnDTO() {
            DrugEntity e = DrugEntity.builder().drugId(1).drugName("Aspirin").build();
            DrugResponse dto = DrugResponse.builder().drugId(1).drugName("Aspirin").build();
            when(drugRepository.findByDrugIdAndOwnerUserId(1, TEST_USER_ID)).thenReturn(Optional.of(e));
            when(drugMapper.mapToDTO(e)).thenReturn(dto);
            assertThat(drugService.getDrugById(1)).isEqualTo(dto);
            verify(drugMapper).mapToDTO(e);
        }
    }

    // ---------------------- addNewDrug ----------------------
    @Nested
    @DisplayName("addNewDrug")
    class AddNewDrug {
        @Test
        void shouldPersistAndReturnDTO() {
            DrugCreateRequest req = new DrugCreateRequest("Ibuprofen", DrugFormDTO.GEL.name(), 2025, 5, "Painkiller");
            DrugFormEntity form = DrugFormEntity.builder().id(7).name("GEL").build();
            DrugEntity saved = DrugEntity.builder().drugId(11).drugName("Ibuprofen").drugForm(form).build();
            DrugResponse expected = DrugResponse.builder().drugId(11).drugName("Ibuprofen").build();

            when(drugFormService.resolve(DrugFormDTO.GEL)).thenReturn(form);
            when(drugRepository.save(any(DrugEntity.class))).thenReturn(saved);
            when(drugMapper.mapToDTO(saved)).thenReturn(expected);

            DrugResponse out = drugService.addNewDrug(req);

            assertThat(out).isEqualTo(expected);
            ArgumentCaptor<DrugEntity> captor = ArgumentCaptor.forClass(DrugEntity.class);
            verify(drugRepository).save(captor.capture());
            assertThat(captor.getValue().getDrugName()).isEqualTo("Ibuprofen");
            assertThat(captor.getValue().getDrugForm()).isEqualTo(form);
        }

        @Test
        void shouldThrow_whenFormIsNull() {
            DrugCreateRequest req = new DrugCreateRequest("Ibuprofen", null, 2025, 5, "Painkiller");
            assertThatThrownBy(
                    () -> drugService.addNewDrug(req)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Drug form cannot be null");
            verifyNoInteractions(drugRepository, drugFormService);
        }

        @Test
        void shouldThrow_whenFormIsInvalid() {
            // given
            DrugCreateRequest req = new DrugCreateRequest("Ibuprofen", "XYZ", 2025, 5, "Painkiller");

            // when + then
            assertThatThrownBy(() -> drugService.addNewDrug(req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid drug form");

            verifyNoInteractions(drugRepository, drugFormService);
        }
    }

    // ---------------------- deleteDrug ----------------------
    @Nested
    @DisplayName("deleteDrug")
    class DeleteDrug {
        @Test
        void shouldDeleteWhenExists() {
            DrugEntity e = DrugEntity.builder().drugId(5).build();
            when(drugRepository.findByDrugIdAndOwnerUserId(5, TEST_USER_ID)).thenReturn(Optional.of(e));
            drugService.deleteDrug(5);
            verify(drugRepository).delete(e);
        }

        @Test
        void shouldThrowWhenMissing() {
            when(drugRepository.findByDrugIdAndOwnerUserId(5, TEST_USER_ID)).thenReturn(Optional.empty());
            assertThrows(DrugNotFoundException.class, () -> drugService.deleteDrug(5));
            verify(drugRepository, never()).delete(any(DrugEntity.class));
        }
    }

    // ---------------------- updateDrug ----------------------
    @Nested
    @DisplayName("updateDrug")
    class UpdateDrug {
        @Test
        void shouldUpdateFieldsAndSave() {
            DrugEntity existing = DrugEntity.builder().drugId(3).drugName("Old").build();
            when(drugRepository.findByDrugIdAndOwnerUserId(3, TEST_USER_ID)).thenReturn(Optional.of(existing));
            DrugFormEntity form = DrugFormEntity.builder().id(2).name("PILLS").build();
            when(drugFormService.resolve(DrugFormDTO.PILLS)).thenReturn(form);

            DrugCreateRequest req = new DrugCreateRequest("New", "PILLS", YEAR_NOW_PLUS_1, 1, "Desc");
            when(drugRepository.save(any(DrugEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            drugService.updateDrug(3, req);

            ArgumentCaptor<DrugEntity> captor = ArgumentCaptor.forClass(DrugEntity.class);
            verify(drugRepository).save(captor.capture());
            DrugEntity saved = captor.getValue();
            assertThat(saved.getDrugName()).isEqualTo("New");
            assertThat(saved.getDrugForm()).isEqualTo(form);
            assertThat(saved.getExpirationDate()).isEqualTo(DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 1));
            assertThat(saved.getDrugDescription()).isEqualTo("Desc");
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(drugRepository.findByDrugIdAndOwnerUserId(77, TEST_USER_ID)).thenReturn(Optional.empty());
            assertThrows(DrugNotFoundException.class, () -> drugService.updateDrug(77, new DrugCreateRequest()));
        }

        @Test
        void shouldThrow_whenFormIsNull_onUpdate() {
            DrugEntity existing = DrugEntity.builder().drugId(3).drugName("Old").build();
            when(drugRepository.findByDrugIdAndOwnerUserId(3, TEST_USER_ID)).thenReturn(Optional.of(existing));

            DrugCreateRequest req = new DrugCreateRequest("New", null, YEAR_NOW_PLUS_1, 1, "Desc");

            assertThatThrownBy(() -> drugService.updateDrug(3, req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Drug form cannot be null");

            verify(drugRepository, never()).save(any(DrugEntity.class));
            verifyNoInteractions(drugFormService);
        }

        @Test
        void shouldThrow_whenFormIsInvalid_onUpdate() {
            // given
            DrugEntity existing = DrugEntity.builder().drugId(3).drugName("Old").build();
            when(drugRepository.findByDrugIdAndOwnerUserId(3, TEST_USER_ID)).thenReturn(Optional.of(existing));
            DrugCreateRequest req = new DrugCreateRequest("New", "XYZ", YEAR_NOW_PLUS_1, 1, "Desc");

            // when + then
            assertThatThrownBy(() -> drugService.updateDrug(3, req)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid drug form");

            verify(drugRepository, never()).save(any(DrugEntity.class));
            verifyNoInteractions(drugFormService);
        }
    }

    // ---------------------- sendExpiryAlertEmails ----------------------
    @Nested
    @DisplayName("sendExpiryAlertEmails")
    class SendExpiryAlerts {
        @Test
        void shouldSendAndMarkAlertSent() {
            OffsetDateTime end = DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 8);
            DrugEntity d = DrugEntity.builder().drugId(1).drugName("Old Drug").expirationDate(end).alertSent(false).build();
            when(drugRepository.findByOwnerUserIdAndExpirationDateLessThanEqualAndAlertSentFalse(TEST_USER_ID, end)).thenReturn(List.of(d));
            when(drugRepository.save(any(DrugEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            drugService.sendExpiryAlertEmailsForCurrentUser(YEAR_NOW_PLUS_1, 8);

            verify(emailService).sendEmail(eq(TEST_USER_EMAIL), anyString(), anyString());
            verify(drugRepository).save(argThat(DrugEntity::isAlertSent));
        }

        @Test
        void shouldWrapAndPropagateWhenEmailFails() {
            OffsetDateTime end = DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 8);
            DrugEntity d = DrugEntity.builder().drugId(1).drugName("X").expirationDate(end).alertSent(false).build();
            when(drugRepository.findByOwnerUserIdAndExpirationDateLessThanEqualAndAlertSentFalse(TEST_USER_ID, end)).thenReturn(List.of(d));
            doThrow(new RuntimeException("smtp fail")).when(emailService).sendEmail(anyString(), anyString(), anyString());
            assertThatThrownBy(() -> drugService.sendExpiryAlertEmailsForCurrentUser(YEAR_NOW_PLUS_1, 8)).isInstanceOf(EmailSendingException.class).hasMessageContaining("Could not send consolidated email alert");
            verify(drugRepository, never()).save(any(DrugEntity.class));
        }

        @Test
        void defaultVariantShouldDelegateToMonthAhead() {
            DrugRepository drugRepository = Mockito.mock(DrugRepository.class);
            CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
            DrugMapper drugMapper = Mockito.mock(DrugMapper.class); // jeśli go masz
            EmailService emailService = Mockito.mock(EmailService.class); // jeśli go masz
            // Dodaj tu inne mocki, jeśli Twój konstruktor ich wymaga

            // 2. Tworzymy ręcznie instancję serwisu, przekazując te mocki
            DrugService serviceInstance = new DrugService(drugRepository, drugFormService, drugMapper, emailService, currentUserService, userRepository, userService);

            // 3. Robimy szpiega na czystym obiekcie
            DrugService spy = Mockito.spy(serviceInstance);

            // 4. Mówimy szpiegowi: "Gdy zawołasz metodę z parametrami, nie rób nic i zwróć 0"
            // Używamy doReturn, żeby uniknąć wywołania prawdziwej logiki wewnątrz szpiega
            doReturn(0).when(spy).sendExpiryAlertEmailsForCurrentUser(anyInt(), anyInt());

            // 5. Wywołujemy metodę domyślną
            spy.sendDefaultExpiryAlertEmailsForCurrentUser();

            // 6. Sprawdzamy, czy "pod maską" wywołała się ta druga metoda
            verify(spy).sendExpiryAlertEmailsForCurrentUser(anyInt(), anyInt());
        }

        @Test
        void shouldSkipAlreadyAlertedDrugs() {
            // given
            OffsetDateTime end = DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 8);
            DrugEntity alreadyAlerted = DrugEntity.builder().drugId(9).drugName("Notified Drug").expirationDate(end).drugDescription("desc").alertSent(true).build();

            // Although the repository method name implies alertSent=false, we purposely return a true value
            // to exercise the defensive else-branch in the service.
            when(drugRepository.findByOwnerUserIdAndExpirationDateLessThanEqualAndAlertSentFalse(TEST_USER_ID, end)).thenReturn(List.of(alreadyAlerted));

            // when
            drugService.sendExpiryAlertEmailsForCurrentUser(YEAR_NOW_PLUS_1, 8);

            // then
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
            verify(drugRepository, never()).save(any(DrugEntity.class));
        }
    }

    // ---------------------- getDrugStatistics ----------------------
    @Nested
    @DisplayName("getDrugStatistics")
    class GetDrugStatistics {
        @Test
        void shouldReturnAggregates() {
            when(drugRepository.countByOwnerUserId(TEST_USER_ID)).thenReturn(10L);
            when(drugRepository.countByOwnerUserIdAndExpirationDateBefore(eq(TEST_USER_ID), any())).thenReturn(4L);
            when(drugRepository.countByOwnerUserIdAndAlertSentTrue(TEST_USER_ID)).thenReturn(3L);
            when(drugRepository.countGroupedByFormAndUserId(TEST_USER_ID)).thenReturn(List.of(new Object[]{"PILLS", 6L}, new Object[]{"GEL", 1L}));
            DrugStatistics s = drugService.getDrugStatistics();
            assertThat(s.getTotalDrugs()).isEqualTo(10);
            assertThat(s.getExpiredDrugs()).isEqualTo(4);
            assertThat(s.getActiveDrugs()).isEqualTo(6);
            assertThat(s.getAlertSentCount()).isEqualTo(3);
            assertThat(s.getDrugsByForm()).containsExactlyInAnyOrderEntriesOf(Map.of("PILLS", 6L, "GEL", 1L));
        }

        @Test
        void shouldIgnoreNullsInGroupedData() {
            when(drugRepository.countByOwnerUserId(TEST_USER_ID)).thenReturn(5L);
            when(drugRepository.countByOwnerUserIdAndExpirationDateBefore(eq(TEST_USER_ID), any())).thenReturn(1L);
            when(drugRepository.countByOwnerUserIdAndAlertSentTrue(TEST_USER_ID)).thenReturn(0L);
            when(drugRepository.countGroupedByFormAndUserId(TEST_USER_ID)).thenReturn(List.of(new Object[]{"PILLS", 2L}, new Object[]{null, 3L}, new Object[]{"GEL", null}));

            DrugStatistics s = drugService.getDrugStatistics();

            assertThat(s.getDrugsByForm()).containsExactlyInAnyOrderEntriesOf(Map.of("PILLS", 2L));
        }
    }

    // ---------------------- searchDrugs ----------------------
    @Nested
    @DisplayName("searchDrugs")
    class SearchDrugs {
        @Test
        void shouldDefaultYearWhenOnlyMonthProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));
            drugService.searchDrugs("", null, null, null, 8, pageable);
            verify(drugRepository).findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable));
        }

        @Test
        void shouldDefaultMonthToDecemberWhenOnlyYearProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));
            drugService.searchDrugs("", null, null, YEAR_NOW_PLUS_1, null, pageable);
            verify(drugRepository).findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable));
        }

        @Test
        void shouldResolveFormAndMapResults() {
            Pageable pageable = PageRequest.of(0, 2);
            DrugFormEntity form = DrugFormEntity.builder().id(9).name("PILLS").build();
            when(drugFormService.resolve(DrugFormDTO.PILLS)).thenReturn(form);

            DrugEntity e1 = DrugEntity.builder().drugId(1).drugName("A2").build();
            DrugEntity e2 = DrugEntity.builder().drugId(2).drugName("B2").build();
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 2));

            DrugResponse d1 = DrugResponse.builder().drugId(1).drugName("A2").build();
            DrugResponse d2 = DrugResponse.builder().drugId(2).drugName("B2").build();
            when(drugMapper.mapToDTO(any(DrugEntity.class))).thenReturn(d1, d2);

            Page<DrugResponse> page = drugService.searchDrugs("", "PILLS", false, null, null, pageable);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).extracting(DrugResponse::getDrugId, DrugResponse::getDrugName).containsExactly(tuple(1, "A2"), tuple(2, "B2"));
        }

        @Test
        void shouldThrowOnInvalidFormValue() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() -> drugService.searchDrugs("", "UNKNOWN", null, null, null, pageable)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid drug form");
        }

        @Test
        void shouldHandleExpiredTrue() {
            Pageable pageable = PageRequest.of(0, 10);
            DrugEntity e = DrugEntity.builder().drugId(1).drugName("Old").build();

            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(e), pageable, 1));
            when(drugMapper.mapToDTO(e)).thenReturn(DrugResponse.builder().drugId(1).drugName("Old").build());

            Page<DrugResponse> page = drugService.searchDrugs("", null, true, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent()).extracting(DrugResponse::getDrugId, DrugResponse::getDrugName).containsExactly(tuple(1, "Old"));
        }

        @Test
        void shouldHandleExpiredFalse() {
            Pageable pageable = PageRequest.of(0, 10);
            DrugEntity e = DrugEntity.builder().drugId(2).drugName("Fresh").build();

            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(e), pageable, 1));
            when(drugMapper.mapToDTO(e)).thenReturn(DrugResponse.builder().drugId(2).drugName("Fresh").build());

            Page<DrugResponse> page = drugService.searchDrugs("", null, false, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent()).extracting(DrugResponse::getDrugId, DrugResponse::getDrugName).containsExactly(tuple(2, "Fresh"));
        }

        @Test
        void shouldAcceptYearAndMonthTogether() {
            Pageable pageable = PageRequest.of(0, 10);

            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<DrugResponse> page = drugService.searchDrugs("", null, null, YEAR_NOW_PLUS_1, 3, pageable);

            assertThat(page).isNotNull();
            verify(drugRepository).findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable));
        }

        @Test
        void shouldFilterByNameCaseInsensitive_smoke() {
            // NOTE: To verify actual DB filtering you need an integration test.
            // Here we verify wiring: non-blank name is accepted, repo is called with a Specification, and results are mapped.
            Pageable pageable = PageRequest.of(0, 10);
            DrugEntity e = DrugEntity.builder().drugId(10).drugName("Profen").build();
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(e), pageable, 1));
            when(drugMapper.mapToDTO(e)).thenReturn(DrugResponse.builder().drugId(10).drugName("Profen").build());

            Page<DrugResponse> page = drugService.searchDrugs("  proF  ", null, null, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent()).extracting(DrugResponse::getDrugId, DrugResponse::getDrugName).containsExactly(tuple(10, "Profen"));
            verify(drugRepository).findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable));
        }
    }
}