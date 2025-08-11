package com.firstaid.unit.service;

import com.firstaid.controller.dto.DrugDTO;
import com.firstaid.controller.dto.DrugFormDTO;
import com.firstaid.controller.dto.DrugRequestDTO;
import com.firstaid.controller.dto.DrugStatisticsDTO;
import com.firstaid.controller.exception.DrugNotFoundException;
import com.firstaid.controller.exception.EmailSendingException;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import com.firstaid.infrastructure.database.mapper.DrugMapper;
import com.firstaid.infrastructure.database.repository.DrugRepository;
import com.firstaid.infrastructure.email.EmailService;
import com.firstaid.infrastructure.util.DateUtils;
import com.firstaid.service.DrugFormService;
import com.firstaid.service.DrugService;
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
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private DrugRepository drugRepository;
    @Mock
    private DrugFormService drugFormService;
    @Mock
    private DrugMapper drugMapper;
    @Mock
    private EmailService emailService;

    @InjectMocks private DrugService drugService;

    static int YEAR_NOW_PLUS_1 = OffsetDateTime.now().plusYears(1).getYear();

    // ---------------------- getDrugById ----------------------
    @Nested @DisplayName("getDrugById")
    class GetDrugById {
        @Test
        void shouldThrowWhenNotFound() {
            when(drugRepository.findById(999)).thenReturn(Optional.empty());
            assertThrows(DrugNotFoundException.class, () -> drugService.getDrugById(999));
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            verify(drugRepository).findById(captor.capture());
            assertThat(captor.getValue()).isEqualTo(999);
            verify(drugRepository).findById(999);
        }
        @Test
        void shouldReturnDTO() {
            DrugEntity e = DrugEntity.builder().drugId(1).drugName("Aspirin").build();
            DrugDTO dto = DrugDTO.builder().drugId(1).drugName("Aspirin").build();
            when(drugRepository.findById(1)).thenReturn(Optional.of(e));
            when(drugMapper.mapToDTO(e)).thenReturn(dto);
            assertThat(drugService.getDrugById(1)).isEqualTo(dto);
            verify(drugMapper).mapToDTO(e);
        }
    }

    // ---------------------- addNewDrug ----------------------
    @Nested @DisplayName("addNewDrug")
    class AddNewDrug {
        @Test
        void shouldPersistAndReturnDTO() {
            DrugRequestDTO req = new DrugRequestDTO("Ibuprofen", DrugFormDTO.GEL.name(), 2025, 5, "Painkiller");
            DrugFormEntity form = DrugFormEntity.builder().id(7).name("GEL").build();
            DrugEntity saved = DrugEntity.builder().drugId(11).drugName("Ibuprofen").drugForm(form).build();
            DrugDTO expected = DrugDTO.builder().drugId(11).drugName("Ibuprofen").build();

            when(drugFormService.resolve(DrugFormDTO.GEL)).thenReturn(form);
            when(drugRepository.save(any(DrugEntity.class))).thenReturn(saved);
            when(drugMapper.mapToDTO(saved)).thenReturn(expected);

            DrugDTO out = drugService.addNewDrug(req);

            assertThat(out).isEqualTo(expected);
            ArgumentCaptor<DrugEntity> captor = ArgumentCaptor.forClass(DrugEntity.class);
            verify(drugRepository).save(captor.capture());
            assertThat(captor.getValue().getDrugName()).isEqualTo("Ibuprofen");
            assertThat(captor.getValue().getDrugForm()).isEqualTo(form);
        }

        @Test
        void shouldThrow_whenFormIsNull() {
            DrugRequestDTO req = new DrugRequestDTO("Ibuprofen", null, 2025, 5,
                    "Painkiller");

            assertThatThrownBy(() -> drugService.addNewDrug(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Drug form cannot be null");

            verifyNoInteractions(drugRepository, drugFormService);
        }

        @Test
        void shouldThrow_whenFormIsInvalid() {
            // given
            DrugRequestDTO req = new DrugRequestDTO("Ibuprofen", "XYZ", 2025, 5,
                    "Painkiller");

            // when + then
            assertThatThrownBy(() -> drugService.addNewDrug(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid drug form");

            verifyNoInteractions(drugRepository, drugFormService);
        }
    }

    // ---------------------- deleteDrug ----------------------
    @Nested @DisplayName("deleteDrug")
    class DeleteDrug {
        @Test
        void shouldDeleteWhenExists() {
            DrugEntity e = DrugEntity.builder().drugId(5).build();
            when(drugRepository.findById(5)).thenReturn(Optional.of(e));
            drugService.deleteDrug(5);
            verify(drugRepository).delete(e);
        }
        @Test
        void shouldThrowWhenMissing() {
            when(drugRepository.findById(5)).thenReturn(Optional.empty());
            assertThrows(DrugNotFoundException.class, () -> drugService.deleteDrug(5));
            verify(drugRepository, never()).delete(any(DrugEntity.class));
        }
    }

    // ---------------------- updateDrug ----------------------
    @Nested @DisplayName("updateDrug")
    class UpdateDrug {
        @Test
        void shouldUpdateFieldsAndSave() {
            DrugEntity existing = DrugEntity.builder().drugId(3).drugName("Old").build();
            when(drugRepository.findById(3)).thenReturn(Optional.of(existing));
            DrugFormEntity form = DrugFormEntity.builder().id(2).name("PILLS").build();
            when(drugFormService.resolve(DrugFormDTO.PILLS)).thenReturn(form);

            DrugRequestDTO req = new DrugRequestDTO("New", "PILLS",
                    YEAR_NOW_PLUS_1, 1, "Desc");
            when(drugRepository.save(any(DrugEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            drugService.updateDrug(3, req);

            ArgumentCaptor<DrugEntity> captor = ArgumentCaptor.forClass(DrugEntity.class);
            verify(drugRepository).save(captor.capture());
            DrugEntity saved = captor.getValue();
            assertThat(saved.getDrugName()).isEqualTo("New");
            assertThat(saved.getDrugForm()).isEqualTo(form);
            assertThat(saved.getExpirationDate()).isEqualTo(DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1,1));
            assertThat(saved.getDrugDescription()).isEqualTo("Desc");
        }
        @Test
        void shouldThrowWhenNotFound() {
            when(drugRepository.findById(77)).thenReturn(Optional.empty());
            assertThrows(DrugNotFoundException.class, () -> drugService.updateDrug(77, new DrugRequestDTO()));
        }

        @Test
        void shouldThrow_whenFormIsNull_onUpdate() {
            DrugEntity existing = DrugEntity.builder().drugId(3).drugName("Old").build();
            when(drugRepository.findById(3)).thenReturn(Optional.of(existing));

            DrugRequestDTO req = new DrugRequestDTO("New", null, YEAR_NOW_PLUS_1, 1, "Desc");

            assertThatThrownBy(() -> drugService.updateDrug(3, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Drug form cannot be null");

            verify(drugRepository, never()).save(any(DrugEntity.class));
            verifyNoInteractions(drugFormService);
        }

        @Test
        void shouldThrow_whenFormIsInvalid_onUpdate() {
            // given
            DrugEntity existing = DrugEntity.builder().drugId(3).drugName("Old").build();
            when(drugRepository.findById(3)).thenReturn(Optional.of(existing));
            DrugRequestDTO req = new DrugRequestDTO("New", "XYZ", YEAR_NOW_PLUS_1, 1, "Desc");

            // when + then
            assertThatThrownBy(() -> drugService.updateDrug(3, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid drug form");

            verify(drugRepository, never()).save(any(DrugEntity.class));
            verifyNoInteractions(drugFormService);
        }
    }

    // ---------------------- sendExpiryAlertEmails ----------------------
    @Nested @DisplayName("sendExpiryAlertEmails")
    class SendExpiryAlerts {
        @Test
        void shouldSkipWhenNoRecipientConfigured() {
            // default: alertRecipientEmail is blank -> nothing happens
            OffsetDateTime end = DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 8);
            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(end)).thenReturn(List.of(
                    DrugEntity.builder().drugId(1).drugName("A").expirationDate(end).alertSent(false).build()
            ));
            drugService.sendExpiryAlertEmails(YEAR_NOW_PLUS_1, 8);
            verifyNoInteractions(emailService);
            verify(drugRepository, never()).save(any(DrugEntity.class));
        }
        @Test
        void shouldSendAndMarkAlertSent() {
            ReflectionTestUtils.setField(drugService, "alertRecipientEmail", "user@example.com");
            OffsetDateTime end = DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 8);
            DrugEntity d = DrugEntity.builder().drugId(1).drugName("Old Drug").expirationDate(end).alertSent(false).build();
            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(end)).thenReturn(List.of(d));
            when(drugRepository.save(any(DrugEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            drugService.sendExpiryAlertEmails(YEAR_NOW_PLUS_1, 8);

            verify(emailService).sendEmail(eq("user@example.com"), anyString(), anyString());
            verify(drugRepository).save(argThat(DrugEntity::isAlertSent));
        }
        @Test
        void shouldWrapAndPropagateWhenEmailFails() {
            ReflectionTestUtils.setField(drugService, "alertRecipientEmail", "user@example.com");
            OffsetDateTime end = DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 8);
            DrugEntity d = DrugEntity.builder().drugId(1).drugName("X").expirationDate(end).alertSent(false).build();
            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(end)).thenReturn(List.of(d));
            doThrow(new RuntimeException("smtp fail")).when(emailService).sendEmail(anyString(), anyString(), anyString());
            assertThatThrownBy(() -> drugService.sendExpiryAlertEmails(YEAR_NOW_PLUS_1, 8))
                    .isInstanceOf(EmailSendingException.class)
                    .hasMessageContaining("Could not send email alert for drug");
            verify(drugRepository, never()).save(any(DrugEntity.class));
        }
        @Test
        void defaultVariantShouldDelegateToMonthAhead() {
            DrugService spy = Mockito.spy(drugService);
            doNothing().when(spy).sendExpiryAlertEmails(anyInt(), anyInt());
            spy.sendDefaultExpiryAlertEmails();
            verify(spy).sendExpiryAlertEmails(anyInt(), anyInt());
        }

        @Test
        void shouldSkipAlreadyAlertedDrugs() {
            // given
            ReflectionTestUtils.setField(drugService, "alertRecipientEmail", "user@example.com");
            OffsetDateTime end = DateUtils.buildExpirationDate(YEAR_NOW_PLUS_1, 8);
            DrugEntity alreadyAlerted = DrugEntity.builder()
                    .drugId(9)
                    .drugName("Notified Drug")
                    .expirationDate(end)
                    .drugDescription("desc")
                    .alertSent(true)
                    .build();

            // Although the repository method name implies alertSent=false, we purposely return a true value
            // to exercise the defensive else-branch in the service.
            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(end))
                    .thenReturn(List.of(alreadyAlerted));

            // when
            drugService.sendExpiryAlertEmails(YEAR_NOW_PLUS_1, 8);

            // then
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
            verify(drugRepository, never()).save(any(DrugEntity.class));
        }
    }

    // ---------------------- getDrugStatistics ----------------------
    @Nested @DisplayName("getDrugStatistics")
    class GetDrugStatistics {
        @Test
        void shouldReturnAggregates() {
            when(drugRepository.count()).thenReturn(10L);
            when(drugRepository.countByExpirationDateBefore(any())).thenReturn(4L);
            when(drugRepository.countByAlertSentTrue()).thenReturn(3L);
            when(drugRepository.countGroupedByForm()).thenReturn(List.of(new Object[]{"PILLS", 6L}, new Object[]{"GEL", 1L}));
            DrugStatisticsDTO s = drugService.getDrugStatistics();
            assertThat(s.getTotalDrugs()).isEqualTo(10);
            assertThat(s.getExpiredDrugs()).isEqualTo(4);
            assertThat(s.getActiveDrugs()).isEqualTo(6);
            assertThat(s.getAlertSentCount()).isEqualTo(3);
            assertThat(s.getDrugsByForm()).containsExactlyInAnyOrderEntriesOf(Map.of("PILLS",6L,"GEL",1L));
        }

        @Test
        void shouldIgnoreNullsInGroupedData() {
            when(drugRepository.count()).thenReturn(5L);
            when(drugRepository.countByExpirationDateBefore(any())).thenReturn(1L);
            when(drugRepository.countByAlertSentTrue()).thenReturn(0L);
            when(drugRepository.countGroupedByForm()).thenReturn(List.of(
                    new Object[]{"PILLS", 2L},
                    new Object[]{null, 3L},
                    new Object[]{"GEL", null}
            ));

            DrugStatisticsDTO s = drugService.getDrugStatistics();

            assertThat(s.getDrugsByForm()).containsExactlyInAnyOrderEntriesOf(Map.of("PILLS", 2L));
        }
    }

    // ---------------------- searchDrugs ----------------------
    @Nested @DisplayName("searchDrugs")
    class SearchDrugs {
        @Test
        void shouldDefaultYearWhenOnlyMonthProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));
            drugService.searchDrugs("", null, null, null, 8, pageable);
            verify(drugRepository).findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable));
        }
        @Test
        void shouldDefaultMonthToDecemberWhenOnlyYearProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));
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
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(e1, e2), pageable, 2));

            DrugDTO d1 = DrugDTO.builder().drugId(1).drugName("A2").build();
            DrugDTO d2 = DrugDTO.builder().drugId(2).drugName("B2").build();
            when(drugMapper.mapToDTO(any(DrugEntity.class))).thenReturn(d1, d2);

            Page<DrugDTO> page = drugService.searchDrugs("", "PILLS", false, null, null, pageable);
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent())
                .extracting(DrugDTO::getDrugId, DrugDTO::getDrugName)
                .containsExactly(
                    tuple(1, "A2"),
                    tuple(2, "B2")
                );
        }
        @Test
        void shouldThrowOnInvalidFormValue() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() -> drugService.searchDrugs("", "UNKNOWN", null,
                    null, null, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid drug form");
        }

        @Test
        void shouldHandleExpiredTrue() {
            Pageable pageable = PageRequest.of(0, 10);
            DrugEntity e = DrugEntity.builder().drugId(1).drugName("Old").build();

            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(e), pageable, 1));
            when(drugMapper.mapToDTO(e)).thenReturn(DrugDTO.builder().drugId(1).drugName("Old").build());

            Page<DrugDTO> page = drugService.searchDrugs("", null, true, null,
                    null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .extracting(DrugDTO::getDrugId, DrugDTO::getDrugName)
                    .containsExactly(tuple(1, "Old"));
        }

        @Test
        void shouldHandleExpiredFalse() {
            Pageable pageable = PageRequest.of(0, 10);
            DrugEntity e = DrugEntity.builder().drugId(2).drugName("Fresh").build();

            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(e), pageable, 1));
            when(drugMapper.mapToDTO(e)).thenReturn(DrugDTO.builder().drugId(2).drugName("Fresh").build());

            Page<DrugDTO> page = drugService.searchDrugs("", null, false, null,
                    null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .extracting(DrugDTO::getDrugId, DrugDTO::getDrugName)
                    .containsExactly(tuple(2, "Fresh"));
        }

        @Test
        void shouldAcceptYearAndMonthTogether() {
            Pageable pageable = PageRequest.of(0, 10);

            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            Page<DrugDTO> page = drugService.searchDrugs("", null, null, YEAR_NOW_PLUS_1,
                    3, pageable);

            assertThat(page).isNotNull();
            verify(drugRepository).findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable));
        }

        @Test
        void shouldFilterByNameCaseInsensitive_smoke() {
            // NOTE: To verify actual DB filtering you need an integration test.
            // Here we verify wiring: non-blank name is accepted, repo is called with a Specification, and results are mapped.
            Pageable pageable = PageRequest.of(0, 10);
            DrugEntity e = DrugEntity.builder().drugId(10).drugName("Profen").build();
            when(drugRepository.findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(e), pageable, 1));
            when(drugMapper.mapToDTO(e)).thenReturn(DrugDTO.builder().drugId(10).drugName("Profen").build());

            Page<DrugDTO> page = drugService.searchDrugs("  proF  ", null, null, null, null, pageable);

            assertThat(page.getTotalElements()).isEqualTo(1);
            assertThat(page.getContent())
                    .extracting(DrugDTO::getDrugId, DrugDTO::getDrugName)
                    .containsExactly(tuple(10, "Profen"));
            verify(drugRepository).findAll(ArgumentMatchers.<Specification<DrugEntity>>any(), eq(pageable));
        }
    }
}