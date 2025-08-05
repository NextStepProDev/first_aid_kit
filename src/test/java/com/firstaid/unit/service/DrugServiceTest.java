package com.firstaid.unit.service;

import com.firstaid.controller.dto.*;
import com.firstaid.controller.exception.DrugNotFoundException;
import com.firstaid.controller.exception.EmailSendingException;
import com.firstaid.controller.exception.InvalidSortFieldException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private EmailService mailService;

    @InjectMocks
    private DrugService drugService;

    @Nested
    @DisplayName("getDrugById")
    class GetDrugByIdTest {

        @Test
        @DisplayName("Should throw exception when drug is not found")
        void shouldThrowWhenNotFound() {
            // given
            Integer drugId = 999;
            when(drugRepository.findById(drugId)).thenReturn(Optional.empty());

            // when / then
            assertThrows(RuntimeException.class, () -> drugService.getDrugById(drugId));
            ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
            verify(drugRepository).findById(captor.capture());
            assertThat(captor.getValue()).isEqualTo(drugId);
        }

        @Test
        @DisplayName("Should return DrugDTO when drug is found")
        void shouldReturnDrugDTO_whenFound() {
            // given
            Integer drugId = 1;
            DrugEntity entity = new DrugEntity();
            entity.setDrugId(drugId);
            entity.setDrugName("Aspirin");
            entity.setExpirationDate(DateUtils.buildExpirationDate(2025, 1));
            entity.setDrugDescription("Painkiller");

            DrugDTO expectedDTO = DrugDTO.builder().drugId(drugId).drugName("Aspirin").expirationDate(DateUtils.buildExpirationDate(2025, 1)).drugDescription("Painkiller").build();

            when(drugRepository.findById(drugId)).thenReturn(Optional.of(entity));
            when(drugMapper.mapToDTO(entity)).thenReturn(expectedDTO);

            // when
            DrugDTO result = drugService.getDrugById(drugId);

            // then
            assertThat(result).isEqualTo(expectedDTO);
            verify(drugMapper).mapToDTO(entity);
        }
    }

    @Nested
    @DisplayName("addNewDrug")
    class AddNewDrugTest {

        @Test
        @DisplayName("Should save a new drug")
        void shouldSaveDrug() {
            // given
            DrugFormDTO gel = DrugFormDTO.GEL;
            DrugRequestDTO dto = new DrugRequestDTO("Ibuprofen", gel.name(), 2025, 5, "Painkiller");

            DrugFormEntity form = DrugFormEntity.builder().id(1).name(DrugFormDTO.GEL.name()).build();
            when(drugFormService.resolve(gel)).thenReturn(form);

            // when
            drugService.addNewDrug(dto);

            // then
            ArgumentCaptor<DrugEntity> captor = ArgumentCaptor.forClass(DrugEntity.class);
            verify(drugRepository).save(captor.capture());
            DrugEntity savedEntity = captor.getValue();

            assertThat(savedEntity.getDrugName()).isEqualTo("Ibuprofen");
            assertThat(savedEntity.getDrugForm()).isEqualTo(form);
            assertThat(savedEntity.getExpirationDate()).isEqualTo(DateUtils.buildExpirationDate(2025, 5));
            assertThat(savedEntity.getDrugDescription()).isEqualTo("Painkiller");
        }
    }

    @Nested
    @DisplayName("deleteDrug")
    class DeleteDrugTest {

        @Test
        @DisplayName("Should delete existing drug")
        void shouldDeleteExistingDrug() {
            // given
            DrugEntity entity = new DrugEntity();
            entity.setDrugId(123);
            when(drugRepository.findById(123)).thenReturn(Optional.of(entity));

            // when
            drugService.deleteDrug(123);

            // then
            verify(drugRepository).findById(123);
            verify(drugRepository).delete(entity);
        }

        @Test
        @DisplayName("Should throw exception when deleting a non-existing drug")
        void shouldThrowWhenDeletingNonExistingDrug(){
            // given
            when(drugRepository.findById(999)).thenReturn(Optional.empty());

            // when / then
            assertThrows(RuntimeException.class, () -> drugService.deleteDrug(999));
            verify(drugRepository).findById(999);
            verify(drugRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateDrug")
    class UpdateDrugTest {

        @Test
        @DisplayName("Should update and return DrugDTO")
        void shouldUpdateAndReturnDTO() {
            // given
            Integer id = 100;
            DrugEntity existing = new DrugEntity();
            existing.setDrugId(id);
            existing.setDrugName("Old Name");

            DrugRequestDTO updateDTO = new DrugRequestDTO("New Name", "PILLS", 2026, 1, "Updated Description");

            DrugFormDTO drugFormDTO = DrugFormDTO.PILLS;
            DrugEntity resolvedForm = DrugEntity.builder().drugId(1).build();

            DrugDTO expectedDTO = DrugDTO.builder().drugId(id).drugName("New Name").drugForm(drugFormDTO).expirationDate(DateUtils.buildExpirationDate(2026, 1)).drugDescription("Updated Description").build();

            when(drugRepository.findById(id)).thenReturn(Optional.of(existing));
            when(drugFormService.resolve(DrugFormDTO.PILLS)).thenReturn(resolvedForm.getDrugForm());
            when(drugRepository.save(any())).thenReturn(existing);
            when(drugMapper.mapToDTO(any())).thenReturn(expectedDTO);

            // when
            DrugDTO result = drugService.updateDrug(id, updateDTO);

            // then
            assertThat(result).isEqualTo(expectedDTO);
            verify(drugRepository).save(any());
        }

        @Test
        @DisplayName("Should throw exception when drug to update is not found")
        void updateDrug_shouldThrow_whenDrugNotFound() {
            when(drugRepository.findById(123)).thenReturn(Optional.empty());

            assertThrows(DrugNotFoundException.class, () -> drugService.updateDrug(123, new DrugRequestDTO()));
            verify(drugRepository).findById(123);
        }
    }

    @Nested
    @DisplayName("getDrugStatistics")
    class GetDrugStatisticsTest {

        @Test
        @DisplayName("Should generate correct drug statistics")
        void shouldGenerateCorrectStatistics() {
            // given
            when(drugRepository.count()).thenReturn(10L);
            when(drugRepository.countByExpirationDateBefore(any())).thenReturn(4L);
            when(drugRepository.countByAlertSentTrue()).thenReturn(2L);
            when(drugRepository.countGroupedByForm()).thenReturn(List.of(new Object[]{"PILLS", 5L}, new Object[]{"SYRUP", 3L}));

            // when
            DrugStatisticsDTO result = drugService.getDrugStatistics();

            // then
            assertThat(result.getTotalDrugs()).isEqualTo(10L);
            assertThat(result.getExpiredDrugs()).isEqualTo(4L);
            assertThat(result.getActiveDrugs()).isEqualTo(6L);
            assertThat(result.getAlertSentCount()).isEqualTo(2L);
            assertThat(result.getDrugsByForm()).containsExactlyInAnyOrderEntriesOf(Map.of("PILLS", 5L, "SYRUP", 3L));
        }
        @Test
        void shouldIgnoreNullValuesInGroupedStats() {
            // given
            Object[] valid = new Object[]{"PILLS", 5L};
            Object[] nullValue = new Object[]{"SYRUP", null}; // arr[1] == null
            Object[] nullKey = new Object[]{null, 10L};       // arr[0] == null

            when(drugRepository.count()).thenReturn(15L);
            when(drugRepository.countByExpirationDateBefore(any())).thenReturn(3L);
            when(drugRepository.countByAlertSentTrue()).thenReturn(2L);
            when(drugRepository.countGroupedByForm()).thenReturn(List.of(valid, nullValue, nullKey));

            // when
            DrugStatisticsDTO result = drugService.getDrugStatistics();

            // then
            assertThat(result.getDrugsByForm())
                    .containsOnly(Map.entry("PILLS", 5L))
                    .doesNotContainKey(null)
                    .doesNotContainValue(null);

            assertThat(result.getTotalDrugs()).isEqualTo(15);
            assertThat(result.getExpiredDrugs()).isEqualTo(3);
            assertThat(result.getAlertSentCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getAllSorted")
    class GetAllSortedTest {

        @Test
        @DisplayName("Should return sorted list of drugs")
        void shouldReturnSortedList() {
            // given
            DrugEntity drug1 = new DrugEntity();
            drug1.setDrugName("Aspirin");
            DrugEntity drug2 = new DrugEntity();
            drug2.setDrugName("Ibuprofen");

            when(drugRepository.findAll(any(Sort.class))).thenReturn(List.of(drug2, drug1));
            when(drugMapper.mapToDTO(drug2)).thenReturn(DrugDTO.builder().drugName("Ibuprofen").build());
            when(drugMapper.mapToDTO(drug1)).thenReturn(DrugDTO.builder().drugName("Aspirin").build());

            // when
            List<DrugDTO> result = drugService.getAllSorted("expirationDate", SortDirectionDTO.ASC);

            // then
            assertThat(result).extracting(DrugDTO::getDrugName).containsExactly("Ibuprofen", "Aspirin");
        }

        @Test
        void getAllSorted_shouldDefaultToASC_whenDirectionIsNull() {
            // when
            drugService.getAllSorted("expirationDate", null);

            // then
            ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
            verify(drugRepository).findAll(sortCaptor.capture());
            Sort capturedSort = sortCaptor.getValue();
            assertNotNull(capturedSort.getOrderFor("expirationDate"));
            assertThat(Objects.requireNonNull(capturedSort.getOrderFor("expirationDate")).getDirection()).isEqualTo(Sort.Direction.ASC);
        }
    }

    @Nested
    @DisplayName("searchByDescription")
    class SearchByDescriptionTest {

        @Test
        @DisplayName("Should return drugs matching description")
        void searchByDescription_shouldReturnMatchingDrugs() {
            // given
            DrugEntity entity = new DrugEntity();
            entity.setDrugName("Nurofen");
            entity.setDrugDescription("Painkiller");

            when(drugRepository.findByDrugDescriptionIgnoreCaseContaining("pain")).thenReturn(List.of(entity));
            when(drugMapper.mapToDTO(entity)).thenReturn(DrugDTO.builder().drugName("Nurofen").drugDescription("Painkiller").build());

            // when
            List<DrugDTO> result = drugService.searchByDescription("pain");

            // then
            assertThat(result).hasSize(1).first().satisfies(dto -> {
                assertThat(dto.getDrugName()).isEqualTo("Nurofen");
                assertThat(dto.getDrugDescription()).isEqualTo("Painkiller");
            });
            verify(drugRepository).findByDrugDescriptionIgnoreCaseContaining("pain");
        }
    }

    @Nested
    @DisplayName("sendExpiryAlertEmails")
    class ExpiryAlertTests {

        @Test
        @DisplayName("Should send email alerts and mark drug as notified")
        void shouldSendEmailAndMarkAlertSent() {
            // given

            OffsetDateTime end = DateUtils.buildExpirationDate(2025, 8);
            DrugEntity drug = DrugEntity.builder().drugId(1).drugName("Old Drug").expirationDate(end).alertSent(false).build();

            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(end)).thenReturn(List.of(drug));
            when(drugRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            drugService.sendExpiryAlertEmails(2025, 8);

            // then
            verify(mailService, times(1)).sendEmail(anyString(), anyString(), anyString());
            verify(drugRepository).save(argThat(DrugEntity::getAlertSent));
            ArgumentCaptor<DrugEntity> captor = ArgumentCaptor.forClass(DrugEntity.class);
            verify(drugRepository).save(captor.capture());
            assertThat(captor.getValue().getAlertSent()).isTrue();
            assertThat(drug.getAlertSent()).isTrue();
        }

        @Test
        @DisplayName("Should throw EmailSendingException when email fails")
        void shouldThrowEmailSendingException_whenEmailFails() {
            // given
            OffsetDateTime end = DateUtils.buildExpirationDate(2025, 5);
            DrugEntity drug = DrugEntity.builder().drugId(1).expirationDate(end).alertSent(false).build();

            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(end)).thenReturn(List.of(drug));
            doThrow(new RuntimeException("Email failed")).when(mailService).sendEmail(any(), any(), any());

            // when / then
            assertThrows(EmailSendingException.class, () -> drugService.sendExpiryAlertEmails(2025, 5));
            verify(drugRepository, never()).save(any());
            EmailSendingException ex = assertThrows(EmailSendingException.class, () -> drugService.sendExpiryAlertEmails(2025, 5));
            assertThat(ex.getMessage()).contains("Could not send email alert for drug");
        }

        @Test
        @DisplayName("Should throw EmailSendingException when emailService fails")
        void sendExpiryAlertEmails_shouldThrowException_whenEmailFails() {
            // given
            OffsetDateTime date = DateUtils.buildExpirationDate(2025, 8);
            DrugEntity drug = DrugEntity.builder().drugId(1).drugName("Apap").expirationDate(date).drugDescription("Ból głowy").alertSent(false).build();

            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(date)).thenReturn(List.of(drug));
            doThrow(new RuntimeException("SMTP error")).when(mailService).sendEmail(any(), any(), any());

            // when / then
            assertThatThrownBy(() -> drugService.sendExpiryAlertEmails(2025, 8)).isInstanceOf(EmailSendingException.class).hasMessageContaining("Could not send email alert for drug");

            verify(mailService).sendEmail(any(), any(), any());
            verify(drugRepository, never()).save(any());
        }

        @Test
        void sendExpiryAlertEmails_shouldProcessMultipleDrugs() {
            OffsetDateTime date = DateUtils.buildExpirationDate(2026, 1);
            DrugEntity drug1 = DrugEntity.builder().drugId(1).drugName("Apap").expirationDate(date).drugDescription("pain relief").alertSent(false).build();

            DrugEntity drug2 = DrugEntity.builder().drugId(2).drugName("Ibuprofen").expirationDate(date).drugDescription("headache").alertSent(false).build();

            List<DrugEntity> drugs = List.of(drug1, drug2);
            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(date)).thenReturn(drugs);

            drugService.sendExpiryAlertEmails(2026, 1);

            verify(mailService, times(2)).sendEmail(any(), any(), any());
            verify(drugRepository, times(2)).save(any());
        }

        @Test
        void sendExpiryAlertEmails_shouldDoNothing_whenNoDrugsToProcess() {
            OffsetDateTime date = DateUtils.buildExpirationDate(2025, 8);
            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(date)).thenReturn(List.of());

            drugService.sendExpiryAlertEmails(2025, 8);

            verify(drugRepository, never()).save(any());
            verify(mailService, never()).sendEmail(any(), any(), any());
        }

        @Test
        void sendExpiryAlertEmails_shouldSkipDrugsWithAlertAlreadySent() {
            OffsetDateTime date = DateUtils.buildExpirationDate(2025, 8);
            DrugEntity alreadyAlerted = DrugEntity.builder()
                    .drugId(1)
                    .drugName("Ibuprofen")
                    .expirationDate(date)
                    .drugDescription("pain")
                    .alertSent(true)
                    .build();
            when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(date))
                    .thenReturn(List.of(alreadyAlerted));

            drugService.sendExpiryAlertEmails(2025, 8);

            verify(drugRepository, never()).save(any());
            verify(mailService, never()).sendEmail(any(), any(), any());
        }


    }

    @Nested
    @DisplayName("getDrugsByName")
    class GetDrugsByNameTest {

        @Test
        @DisplayName("Should return drugs matching name (case-insensitive)")
        void getDrugsByName_shouldReturnMatchingDrugs() {
            // given
            DrugEntity entity = new DrugEntity();
            entity.setDrugName("Paracetamol");
            when(drugRepository.findByDrugNameContainingIgnoreCase("Parace")).thenReturn(List.of(entity));
            when(drugMapper.mapToDTO(entity)).thenReturn(DrugDTO.builder().drugName("Paracetamol").build());

            // when
            List<DrugDTO> result = drugService.getDrugsByName("Parace");

            // then
            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(drugRepository).findByDrugNameContainingIgnoreCase(captor.capture());
            String value = captor.getValue();
            assertThat(value).isNotNull();
            assertThat(value).isEqualTo("Parace");
            assertThat(result).hasSize(1).first().satisfies(dto -> assertThat(dto.getDrugName()).isEqualTo("Paracetamol"));
        }

        @Test
        @DisplayName("Should return empty list when no drugs match given name")
        void getDrugsByName_shouldReturnEmptyList_whenNoMatch() {
            // given
            when(drugRepository.findByDrugNameContainingIgnoreCase("xyz")).thenReturn(List.of());

            // when
            List<DrugDTO> result = drugService.getDrugsByName("xyz");

            // then
            assertThat(result).isNotNull().isEmpty();
            verify(drugRepository).findByDrugNameContainingIgnoreCase("xyz");
            verifyNoInteractions(drugMapper);
        }
    }

    @Nested
    @DisplayName("getDrugsExpiringSoon")
    class GetDrugsExpiringSoonTest {

        @Test
        @DisplayName("Should return drugs with expiration date before or equal to given year and month")
        void shouldReturnDrugsBeforeOrEqualToDate() {
            // given

            int year = OffsetDateTime.now().getYear();
            int month = OffsetDateTime.now().getMonthValue();
            OffsetDateTime dateTime = DateUtils.buildExpirationDate(year, month);
            DrugEntity drug1 = DrugEntity.builder().drugId(1).drugName("Altacet").expirationDate(dateTime).build();
            DrugEntity drug2 = DrugEntity.builder().drugId(2).drugName("Naproxen").expirationDate(dateTime).build();

            when(drugRepository.findByExpirationDateLessThanEqualOrderByExpirationDateAsc(dateTime)).thenReturn(List.of(drug1, drug2));
            when(drugMapper.mapToDTO(drug1)).thenReturn(DrugDTO.builder().drugName("Altacet").build());
            when(drugMapper.mapToDTO(drug2)).thenReturn(DrugDTO.builder().drugName("Naproxen").build());

            // when
            List<DrugDTO> drugsExpiringSoon = drugService.getDrugsExpiringSoon(year, month);

            ArgumentCaptor<OffsetDateTime> captor = ArgumentCaptor.forClass(OffsetDateTime.class);
            verify(drugRepository).findByExpirationDateLessThanEqualOrderByExpirationDateAsc(captor.capture());
            OffsetDateTime capturedDate = captor.getValue();
            assertThat(capturedDate).isNotNull();
            assertThat(capturedDate.getYear()).isEqualTo(year);
            assertThat(capturedDate.getMonthValue()).isEqualTo(month);
            assertThat(drugsExpiringSoon).isNotNull();
            assertThat(drugsExpiringSoon).hasSize(2);
            assertThat(drugsExpiringSoon.get(0).getDrugName()).isEqualTo("Altacet");
            assertThat(drugsExpiringSoon.get(1).getDrugName()).isEqualTo("Naproxen");
            verify(drugRepository).findByExpirationDateLessThanEqualOrderByExpirationDateAsc(any(OffsetDateTime.class));
            verify(drugMapper, times(2)).mapToDTO(any(DrugEntity.class));
        }
    }

    @Nested
    @DisplayName("getExpiredDrugs")
    class GetExpiredDrugsTest {

        @Test
        @DisplayName("Should return drugs that are expired")
        void getExpiredDrugs_shouldReturnDrugsBeforeNow() {
            OffsetDateTime pastDate = OffsetDateTime.now().minusMonths(2);
            DrugEntity drug1 = DrugEntity.builder().drugId(1).drugName("Altacet").expirationDate(pastDate).build();
            DrugEntity drug2 = DrugEntity.builder().drugId(2).drugName("Naproxen").expirationDate(pastDate).build();

            when(drugRepository.findAll()).thenReturn(List.of(drug1, drug2));
            when(drugMapper.mapToDTO(drug1)).thenReturn(DrugDTO.builder().drugName("Altacet").expirationDate(pastDate).build());
            when(drugMapper.mapToDTO(drug2)).thenReturn(DrugDTO.builder().drugName("Naproxen").expirationDate(pastDate).build());

            // when
            List<DrugDTO> expiredDrugs = drugService.getExpiredDrugs();

            // then
            assertThat(expiredDrugs).isNotNull();
            assertThat(expiredDrugs).hasSize(2);
            assertThat(expiredDrugs.get(0).getDrugName()).isEqualTo("Altacet");
            assertThat(expiredDrugs.get(1).getDrugName()).isEqualTo("Naproxen");
            verify(drugRepository).findAll();
            verify(drugMapper, times(2)).mapToDTO(any(DrugEntity.class));
        }

        @Test
        @DisplayName("Should ignore future drugs")
        void getExpiredDrugs_shouldIgnoreFutureDrugs() {
            // given
            DrugEntity expired = DrugEntity.builder().expirationDate(OffsetDateTime.now().minusDays(1)).build();
            DrugEntity future = DrugEntity.builder().expirationDate(OffsetDateTime.now().plusDays(10)).build();

            when(drugRepository.findAll()).thenReturn(List.of(expired, future));
            when(drugMapper.mapToDTO(expired)).thenReturn(DrugDTO.builder().expirationDate(OffsetDateTime.now().minusDays(1)).build());

            // when
            List<DrugDTO> result = drugService.getExpiredDrugs();

            // then
            assertThat(result).hasSize(1);
        }
        @Test
        @DisplayName("Should do nothing when no expired drugs found")
        void getExpiredDrugs_shouldDoNothing_whenNoExpiredDrugsFound() {
            // given
            OffsetDateTime futureDateTime = OffsetDateTime.now().plusDays(10);
            DrugEntity future = DrugEntity.builder().expirationDate(futureDateTime).build();

            when(drugRepository.findAll()).thenReturn(List.of(future));

            // when
            List<DrugDTO> result = drugService.getExpiredDrugs();

            // then
            assertThat(result).hasSize(0);
            verify(drugRepository).findAll();
            verify(drugMapper, never()).mapToDTO(any(DrugEntity.class));
        }

        @Test
        @DisplayName("Should do nothing when no expired drugs found")
        void getExpiredDrugs_shouldDoNothing_whenExpiredTimeIsNull() {
            // given
            DrugEntity.builder().drugName("DrugWithNullExpiration").expirationDate(null).build();
            DrugEntity future = DrugEntity.builder().expirationDate(null).build();

            when(drugRepository.findAll()).thenReturn(List.of(future));

            // when
            List<DrugDTO> result = drugService.getExpiredDrugs();

            // then
            assertThat(result).hasSize(0);
            verify(drugRepository).findAll();
            verify(drugMapper, never()).mapToDTO(any(DrugEntity.class));
        }
    }

    @Nested
    @DisplayName("getAllDrugsSimple")
    class GetAllDrugsSimpleTest {

        @Test
        @DisplayName("Should return mapped drugs as DrugSimpleDTO")
        void getAllDrugsSimple_shouldReturnMappedDrugs() {
            DrugEntity drug1 = new DrugEntity();
            drug1.setDrugId(1);
            drug1.setDrugName("Aspirin");
            DrugEntity drug2 = new DrugEntity();
            drug2.setDrugId(2);
            drug2.setDrugName("Ibuprofen");
            when(drugRepository.findAll()).thenReturn(List.of(drug1, drug2));
            when(drugMapper.mapToSimpleDTO(drug1)).thenReturn(DrugSimpleDTO.builder().drugId(1).drugName("Aspirin").build());
            when(drugMapper.mapToSimpleDTO(drug2)).thenReturn(DrugSimpleDTO.builder().drugId(2).drugName("Ibuprofen").build());

            // when
            List<DrugSimpleDTO> result = drugService.getAllDrugsSimple();

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDrugName()).isEqualTo("Aspirin");
            assertThat(result.get(1).getDrugName()).isEqualTo("Ibuprofen");
            verify(drugRepository).findAll();
            verify(drugMapper, times(2)).mapToSimpleDTO(any(DrugEntity.class));
        }
    }

    @Nested
    @DisplayName("getDrugsPaged")
    class GetDrugsPagedTest {

        @Test
        @DisplayName("Should return paginated list of drugs")
        void shouldReturnPaginatedDrugs() {
            // given
            Pageable pageable = PageRequest.of(0, 2);

            DrugEntity drug1 = DrugEntity.builder().drugId(1).drugName("Altacet").expirationDate(OffsetDateTime.now()).build();
            DrugEntity drug2 = DrugEntity.builder().drugId(2).drugName("Ibuprom").expirationDate(OffsetDateTime.now()).build();

            List<DrugEntity> drugEntities = List.of(drug1, drug2);
            Page<DrugEntity> pageResult = new PageImpl<>(drugEntities, pageable, 2);

            when(drugRepository.findAll(pageable)).thenReturn(pageResult);
            when(drugMapper.mapToDTO(drug1)).thenReturn(DrugDTO.builder().drugName("Altacet").build());
            when(drugMapper.mapToDTO(drug2)).thenReturn(DrugDTO.builder().drugName("Ibuprom").build());

            // when
            Page<DrugDTO> result = drugService.getDrugsPaged(pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getDrugName()).isEqualTo("Altacet");
            assertThat(result.getContent().get(1).getDrugName()).isEqualTo("Ibuprom");

            verify(drugRepository).findAll(pageable);
            verify(drugMapper, times(2)).mapToDTO(any(DrugEntity.class));
        }
    }

    @Nested
    @DisplayName("getDrugsByForm")
    class GetDrugsByFormTest {

        @Test
        @DisplayName("Should return drugs for valid form")
        void getDrugsByForm_shouldReturnDrugsForValidForm() {
            DrugFormDTO formPills = DrugFormDTO.PILLS;
            DrugFormEntity formEntity = DrugFormEntity.builder().id(1).name(formPills.name()).build();
            DrugEntity drug1 = DrugEntity.builder().drugId(1).drugName("Aspirin").drugForm(formEntity).build();
            DrugEntity drug2 = DrugEntity.builder().drugId(2).drugName("Ibuprofen").drugForm(formEntity).build();

            when(drugFormService.resolve(formPills)).thenReturn(formEntity);
            when(drugRepository.findByDrugForm(formEntity)).thenReturn(List.of(drug1, drug2));
            when(drugMapper.mapToDTO(drug1)).thenReturn(DrugDTO.builder().drugName("Aspirin").build());
            when(drugMapper.mapToDTO(drug2)).thenReturn(DrugDTO.builder().drugName("Ibuprofen").build());

            // when
            List<DrugDTO> result = drugService.getDrugsByForm(formPills.name());

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDrugName()).isEqualTo("Aspirin");
            assertThat(result.get(1).getDrugName()).isEqualTo("Ibuprofen");
            verify(drugFormService).resolve(formPills);
            verify(drugRepository).findByDrugForm(formEntity);
            verify(drugMapper).mapToDTO(drug1);
            verify(drugMapper).mapToDTO(drug2);
            verify(drugMapper, times(2)).mapToDTO(any(DrugEntity.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid form")
        void getDrugsByForm_shouldThrowForInvalidForm() {
            // given
            String invalidForm = "UNKNOWN";

            // when + then
            assertThatThrownBy(() -> drugService.getDrugsByForm(invalidForm))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid drug form: "
                            + invalidForm);
        }
    }

    @Nested
    @DisplayName("getAllDrugs")
    class GetAllDrugsTest {

        @Test
        @DisplayName("Should return all drugs")
        void getAllDrugs_shouldReturnAllDrugs() {

            DrugEntity drug1 = DrugEntity.builder().drugId(1).drugName("Aspirin").build();

            DrugEntity drug2 = DrugEntity.builder().drugId(2).drugName("Ibuprofen").build();

            when(drugRepository.findAll()).thenReturn(List.of(drug1, drug2));
            when(drugMapper.mapToDTO(drug1)).thenReturn(DrugDTO.builder().drugId(1).drugName("Aspirin").build());
            when(drugMapper.mapToDTO(drug2)).thenReturn(DrugDTO.builder().drugId(2).drugName("Ibuprofen").build());

            // when
            List<DrugDTO> result = drugService.getAllDrugs();

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDrugName()).isEqualTo("Aspirin");
            assertThat(result.get(1).getDrugName()).isEqualTo("Ibuprofen");
            verify(drugRepository).findAll();
            verify(drugMapper, times(2)).mapToDTO(any(DrugEntity.class));

        }
    }

    @Nested
    @DisplayName("resolveSortField(private method)")
    class ResolveSortFieldTest {

        @Test
        @DisplayName("Should throw exception when invalid sort field is passed")
        void getAllSorted_shouldThrowException_whenSortFieldInvalid() {
            // given
            String invalidField = "unknownField";

            // when / then
            assertThatThrownBy(() -> drugService.getAllSorted(invalidField, SortDirectionDTO.ASC)).isInstanceOf(InvalidSortFieldException.class).hasMessageContaining(invalidField);
        }

        @Test
        @DisplayName("Should resolve valid sort field")
        void getAllSorted_shouldUseCorrectSortField_forForm() {
            // when
            drugService.getAllSorted("form", SortDirectionDTO.ASC);

            // then
            ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
            verify(drugRepository).findAll(sortCaptor.capture());

            Sort sort = sortCaptor.getValue();
            assertThat(sort.getOrderFor("drugForm.name")).isNotNull();
        }

        @Test
        @DisplayName("Should resolve valid sort field for id")
        void getAllSorted_shouldUseCorrectSortField_forId() {
            // when
            drugService.getAllSorted("id", SortDirectionDTO.ASC);

            // then
            ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
            verify(drugRepository).findAll(sortCaptor.capture());

            Sort sort = sortCaptor.getValue();
            assertThat(sort.getOrderFor("drugId")).isNotNull();
        }

        @Test
        @DisplayName("Should resolve valid sort field for description")
        void getAllSorted_shouldUseCorrectSortField_forDescription() {
            // when
            drugService.getAllSorted("description", SortDirectionDTO.DESC);

            // then
            ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
            verify(drugRepository).findAll(sortCaptor.capture());

            Sort sort = sortCaptor.getValue();
            assertThat(sort.getOrderFor("drugDescription")).isNotNull();
            assertThat(Objects.requireNonNull(sort.getOrderFor("drugDescription")).getDirection())
                    .isEqualTo(Sort.Direction.DESC);
        }
    }
}