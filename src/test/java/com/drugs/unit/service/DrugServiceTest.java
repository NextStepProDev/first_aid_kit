package com.drugs.unit.service;

import com.drugs.controller.dto.DrugDTO;
import com.drugs.controller.dto.DrugFormDTO;
import com.drugs.controller.dto.DrugRequestDTO;
import com.drugs.controller.dto.DrugStatisticsDTO;
import com.drugs.infrastructure.database.entity.DrugEntity;
import com.drugs.infrastructure.database.entity.DrugFormEntity;
import com.drugs.infrastructure.database.mapper.DrugMapper;
import com.drugs.infrastructure.database.repository.DrugRepository;
import com.drugs.infrastructure.email.EmailService;
import com.drugs.infrastructure.util.DateUtils;
import com.drugs.service.DrugFormService;
import com.drugs.service.DrugService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @DisplayName("Should throw exception when drug is not found")
    void getDrugById_shouldThrowWhenNotFound() {
        // given
        Integer drugId = 999;
        DrugEntity result = new DrugEntity();
        result.setDrugId(999);
        result.setDrugName("Unknown Drug");

        when(drugRepository.findById(drugId)).thenReturn(Optional.empty());

        // when / then
        assertThrows(RuntimeException.class, () -> drugService.getDrugById(drugId));
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(drugRepository).findById(captor.capture());
        Integer capturedId = captor.getValue();
        assertThat(capturedId).isEqualTo(drugId);
        // Verify that the repository was called with the correct drugId
    }

    @Test
    @DisplayName("Should return DrugDTO when drug is found")
    void getDrugById_shouldReturnDrugDTO_whenFound() {
        // given
        Integer drugId = 1;
        DrugEntity entity = new DrugEntity();
        entity.setDrugId(drugId);
        entity.setDrugName("Aspirin");
        entity.setExpirationDate(DateUtils.buildExpirationDate(2025, 1));
        entity.setDrugDescription("Painkiller");

        when(drugRepository.findById(drugId)).thenReturn(Optional.of(entity));
        when(drugMapper.mapToDTO(entity)).thenReturn(DrugDTO.builder()
                .drugId(drugId)
                .drugName("Aspirin")
                .expirationDate(DateUtils.buildExpirationDate(2025, 1))
                .drugDescription("Painkiller")
                .build());

        // when
        DrugDTO result = drugService.getDrugById(drugId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDrugName()).isEqualTo("Aspirin");
        assertThat(result.getExpirationDate()).isEqualTo(DateUtils.buildExpirationDate(2025, 1));
        assertThat(result.getDrugDescription()).isEqualTo("Painkiller");
        verify(drugMapper).mapToDTO(entity);
    }

    @Test
    @DisplayName("Should save a new drug")
    void addNewDrug_shouldSaveDrug() {
        // given
        DrugFormDTO gel = DrugFormDTO.GEL;
        DrugRequestDTO dto = new DrugRequestDTO(
                "Ibuprofen", gel.name(), 2025, 5, "Painkiller"
        );

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

    @Test
    @DisplayName("Should delete existing drug")
    void deleteDrug_shouldDeleteExistingDrug() {
        // given
        DrugEntity entity = new DrugEntity();
        entity.setDrugId(123);
        when(drugRepository.findById(123)).thenReturn(Optional.of(entity));

        // when
        drugService.deleteDrug(123);

        // then
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(drugRepository).findById(captor.capture());
        Integer capturedId = captor.getValue();
        assertThat(capturedId).isEqualTo(123);
        assertThat(capturedId).isEqualTo(entity.getDrugId());
        verify(drugRepository).delete(entity);
    }

    @Test
    @DisplayName("Should throw exception when deleting a non-existing drug")
    void deleteDrug_shouldThrowWhenNotFound() {
        // given
        when(drugRepository.findById(999)).thenReturn(Optional.empty());

        // when / then
        assertThrows(RuntimeException.class, () -> drugService.deleteDrug(999));
        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(drugRepository).findById(captor.capture());
        Integer capturedId = captor.getValue();
        assertThat(capturedId).isEqualTo(999);
    }

    @Test
    @DisplayName("Should update and return DrugDTO")
    void updateDrug_shouldUpdateAndReturnDTO() {
        // given
        Integer id = 100;
        DrugEntity existing = new DrugEntity();
        existing.setDrugId(id);
        existing.setDrugName("Old Name");

        DrugRequestDTO updateDTO = new DrugRequestDTO(
                "New Name",
                "PILLS",
                2026,
                1,
                "Updated Description"
        );

        DrugFormDTO drugFormDTO = DrugFormDTO.PILLS;
        DrugEntity resolvedForm = DrugEntity.builder().drugId(1).build();

        when(drugRepository.findById(id)).thenReturn(Optional.of(existing));
        when(drugFormService.resolve(DrugFormDTO.PILLS)).thenReturn(resolvedForm.getDrugForm());
        when(drugRepository.save(any())).thenReturn(existing);
        when(drugMapper.mapToDTO(any())).thenReturn(DrugDTO.builder()
                .drugId(id)
                .drugName("New Name")
                .drugForm(drugFormDTO)
                .expirationDate(DateUtils.buildExpirationDate(2026, 1))
                .drugDescription("Updated Description")
                .build());

        // when
        DrugDTO result = drugService.updateDrug(id, updateDTO);

        // then
        ArgumentCaptor<DrugEntity> captor = ArgumentCaptor.forClass(DrugEntity.class);
        verify(drugRepository).save(captor.capture());
        DrugEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getDrugName()).isEqualTo("New Name");
        assertThat(savedEntity.getDrugDescription()).isEqualTo("Updated Description");
        assertThat(result.getDrugName()).isEqualTo("New Name");
        assertThat(result.getDrugDescription()).isEqualTo("Updated Description");
    }


    @Test
    @DisplayName("Should generate correct drug statistics")
    void shouldGenerateCorrectStatistics() {
        when(drugRepository.count()).thenReturn(10L);
        when(drugRepository.countByExpirationDateBefore(any())).thenReturn(4L);
        when(drugRepository.countByAlertSentTrue()).thenReturn(2L);
        when(drugRepository.countGroupedByForm()).thenReturn(
                java.util.List.of(
                        new Object[]{"PILLS", 5L},
                        new Object[]{"SYRUP", 3L}
                )
        );

        DrugStatisticsDTO result = drugService.getDrugStatistics();

        assertThat(result.getTotalDrugs()).isEqualTo(10L);
        assertThat(result.getExpiredDrugs()).isEqualTo(4L);
        assertThat(result.getActiveDrugs()).isEqualTo(6L); // 10 - 4
        assertThat(result.getAlertSentCount()).isEqualTo(2L);
        assertThat(result.getDrugsByForm()).containsEntry("PILLS", 5L);
        assertThat(result.getDrugsByForm()).containsEntry("SYRUP", 3L);
    }

    @Test
    @DisplayName("Should return sorted list of drugs")
    void getDrugsSorted_shouldReturnSortedList() {
        DrugEntity drug1 = new DrugEntity();
        drug1.setDrugName("Aspirin");
        drug1.setExpirationDate(DateUtils.buildExpirationDate(2025, 1));
        DrugEntity drug2 = new DrugEntity();
        drug2.setDrugName("Ibuprofen");
        drug2.setExpirationDate(DateUtils.buildExpirationDate(2024, 12));

        when(drugRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(java.util.List.of(drug2, drug1));
        when(drugMapper.mapToDTO(drug2)).thenReturn(DrugDTO.builder().drugName("Ibuprofen").build());
        when(drugMapper.mapToDTO(drug1)).thenReturn(DrugDTO.builder().drugName("Aspirin").build());

        java.util.List<DrugDTO> result = drugService.getAllSorted("expirationDate");

        assertThat(result.get(0).getDrugName()).isEqualTo("Ibuprofen");
        assertThat(result.get(1).getDrugName()).isEqualTo("Aspirin");
    }

    @Test
    @DisplayName("Should return drugs matching description")
    void getDrugsByDescription_shouldReturnMatchingDrugs() {
        DrugEntity entity = new DrugEntity();
        entity.setDrugName("Nurofen");
        entity.setDrugDescription("Painkiller");

        when(drugRepository.findByDrugDescriptionIgnoreCaseContaining("pain")).thenReturn(java.util.List.of(entity));
        when(drugMapper.mapToDTO(entity)).thenReturn(DrugDTO.builder().drugName("Nurofen").build());

        java.util.List<DrugDTO> result = drugService.searchByDescription("pain");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDrugName()).isEqualTo("Nurofen");
    }

    @Test
    @DisplayName("Should send email alerts and mark drug as notified")
    void sendMailAlert_shouldSendEmailAndMarkAlertSent() {
        // given: przygotowanie leku z datą ważności i bez wysłanego alertu
        DrugEntity drug = new DrugEntity();
        drug.setDrugId(1);
        drug.setDrugName("Old Drug");
        OffsetDateTime end = DateUtils.buildExpirationDate(2025, 5);
        drug.setExpirationDate(end);
        drug.setAlertSent(false);
        drug.setDrugForm(DrugFormEntity.builder().name("PILLS").build());

        // mockowanie repozytorium – zwróci lek zbliżający się do przeterminowania
        when(drugRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(eq(end)))
                .thenReturn(List.of(drug));

        // mockowanie metody wysyłającej e-mail
        doNothing().when(mailService).sendEmail(any(), any(), any());

        // mockowanie zapisu leku po wysłaniu alertu
        when(drugRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when: wykonanie metody wysyłającej alerty
        drugService.sendExpiryAlertEmails(2025, 5);

        // then: sprawdzenie, że e-mail został wysłany dwa razy (na dwa adresy)
        verify(mailService, times(2)).sendEmail(anyString(), anyString(), anyString());

        // sprawdzenie, że lek został zapisany
        ArgumentCaptor<DrugEntity> savedCaptor = ArgumentCaptor.forClass(DrugEntity.class);
        verify(drugRepository).save(savedCaptor.capture());

        // sprawdzenie, że znacznik alertu został ustawiony
        DrugEntity saved = savedCaptor.getValue();
        assertThat(saved.getAlertSent()).isTrue();
    }
}
