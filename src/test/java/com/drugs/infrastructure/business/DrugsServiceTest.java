package com.drugs.infrastructure.business;

import com.drugs.controller.dto.DrugStatisticsDTO;
import com.drugs.controller.dto.DrugsDTO;
import com.drugs.controller.dto.DrugsFormDTO;
import com.drugs.controller.dto.DrugsRequestDTO;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import com.drugs.infrastructure.database.mapper.DrugsMapper;
import com.drugs.infrastructure.database.repository.DrugsRepository;
import com.drugs.infrastructure.mail.EmailService;
import com.drugs.infrastructure.util.DateUtils;
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
class DrugsServiceTest {

    @Mock
    private DrugsRepository drugsRepository;

    @Mock
    private DrugsFormService drugsFormService;

    @Mock
    private DrugsMapper drugsMapper;

    @Mock
    private EmailService mailService;

    @InjectMocks
    private DrugsService drugsService;

    @Test
    void addNewDrug_shouldSaveDrug() {
        // given
        DrugsRequestDTO dto = new DrugsRequestDTO("Ibuprofen", "GEL", 2025, 5, "Painkiller");

        DrugsFormDTO drugsFormDTO = DrugsFormDTO.GEL;
        DrugsEntity resolvedForm = DrugsEntity.builder().drugsId(1).build();

        when(drugsFormService.resolve(DrugsFormDTO.GEL)).thenReturn(resolvedForm.getDrugsForm());

        // when
        drugsService.addNewDrug(dto);

        // then
        ArgumentCaptor<DrugsEntity> captor = ArgumentCaptor.forClass(DrugsEntity.class);
        verify(drugsRepository).save(captor.capture());
        DrugsEntity savedEntity = captor.getValue();

        assertThat(savedEntity.getDrugsName()).isEqualTo("Ibuprofen");
        assertThat(savedEntity.getDrugsForm()).isEqualTo(resolvedForm.getDrugsForm());
        assertThat(savedEntity.getExpirationDate()).isEqualTo(DateUtils.buildExpirationDate(2025, 5));
        assertThat(savedEntity.getDrugsDescription()).isEqualTo("Painkiller");
    }

    @Test
    void deleteDrug_shouldDeleteExistingDrug() {
        // given
        DrugsEntity entity = new DrugsEntity();
        entity.setDrugsId(123);
        when(drugsRepository.findById(123)).thenReturn(Optional.of(entity));

        // when
        drugsService.deleteDrug(123);

        // then
        verify(drugsRepository).delete(entity);
    }

    @Test
    void deleteDrug_shouldThrowWhenNotFound() {
        // given
        when(drugsRepository.findById(999)).thenReturn(Optional.empty());

        // when / then
        assertThrows(RuntimeException.class, () -> drugsService.deleteDrug(999));
    }

    @Test
    void updateDrug_shouldUpdateAndReturnDTO() {
        // given
        Integer id = 100;
        DrugsEntity existing = new DrugsEntity();
        existing.setDrugsId(id);
        existing.setDrugsName("Old Name");

        DrugsRequestDTO updateDTO = new DrugsRequestDTO(
                "New Name",
                "PILLS",
                2026,
                1,
                "Updated Description"
        );

        DrugsFormDTO drugsFormDTO = DrugsFormDTO.PILLS;
        DrugsEntity resolvedForm = DrugsEntity.builder().drugsId(1).build();

        when(drugsRepository.findById(id)).thenReturn(Optional.of(existing));
        when(drugsFormService.resolve(DrugsFormDTO.PILLS)).thenReturn(resolvedForm.getDrugsForm());
        when(drugsRepository.save(any())).thenReturn(existing);
        when(drugsMapper.mapToDTO(any())).thenReturn(DrugsDTO.builder()
                .drugsId(id)
                .drugsName("New Name")
                .drugsForm(drugsFormDTO)
                .expirationDate(DateUtils.buildExpirationDate(2026, 1))
                .drugsDescription("Updated Description")
                .build());

        // when
        DrugsDTO result = drugsService.updateDrug(id, updateDTO);

        // then
        ArgumentCaptor<DrugsEntity> captor = ArgumentCaptor.forClass(DrugsEntity.class);
        verify(drugsRepository).save(captor.capture());
        DrugsEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getDrugsName()).isEqualTo("New Name");
        assertThat(savedEntity.getDrugsDescription()).isEqualTo("Updated Description");
        assertThat(result.getDrugsName()).isEqualTo("New Name");
        assertThat(result.getDrugsDescription()).isEqualTo("Updated Description");
    }


    @Test
    void shouldGenerateCorrectStatistics() {
        when(drugsRepository.count()).thenReturn(10L);
        when(drugsRepository.countByExpirationDateBefore(any())).thenReturn(4L);
        when(drugsRepository.countByAlertSentTrue()).thenReturn(2L);
        when(drugsRepository.countGroupedByForm()).thenReturn(
                java.util.List.of(
                        new Object[]{"PILLS", 5L},
                        new Object[]{"SYRUP", 3L}
                )
        );

        DrugStatisticsDTO result = drugsService.getDrugStatistics();

        assertThat(result.getTotalDrugs()).isEqualTo(10L);
        assertThat(result.getExpiredDrugs()).isEqualTo(4L);
        assertThat(result.getActiveDrugs()).isEqualTo(6L); // 10 - 4
        assertThat(result.getAlertSentCount()).isEqualTo(2L);
        assertThat(result.getDrugsByForm()).containsEntry("PILLS", 5L);
        assertThat(result.getDrugsByForm()).containsEntry("SYRUP", 3L);
    }

    @Test
    void getDrugsSorted_shouldReturnSortedList() {
        DrugsEntity drug1 = new DrugsEntity();
        drug1.setDrugsName("Aspirin");
        drug1.setExpirationDate(DateUtils.buildExpirationDate(2025, 1));
        DrugsEntity drug2 = new DrugsEntity();
        drug2.setDrugsName("Ibuprofen");
        drug2.setExpirationDate(DateUtils.buildExpirationDate(2024, 12));

        when(drugsRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(java.util.List.of(drug2, drug1));
        when(drugsMapper.mapToDTO(drug2)).thenReturn(DrugsDTO.builder().drugsName("Ibuprofen").build());
        when(drugsMapper.mapToDTO(drug1)).thenReturn(DrugsDTO.builder().drugsName("Aspirin").build());

        java.util.List<DrugsDTO> result = drugsService.getAllSorted("expirationDate");

        assertThat(result.get(0).getDrugsName()).isEqualTo("Ibuprofen");
        assertThat(result.get(1).getDrugsName()).isEqualTo("Aspirin");
    }

    @Test
    void getDrugsByDescription_shouldReturnMatchingDrugs() {
        DrugsEntity entity = new DrugsEntity();
        entity.setDrugsName("Nurofen");
        entity.setDrugsDescription("Painkiller");

        when(drugsRepository.findByDrugsDescriptionIgnoreCaseContaining("pain")).thenReturn(java.util.List.of(entity));
        when(drugsMapper.mapToDTO(entity)).thenReturn(DrugsDTO.builder().drugsName("Nurofen").build());

        java.util.List<DrugsDTO> result = drugsService.searchByDescription("pain");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDrugsName()).isEqualTo("Nurofen");
    }

    @Test
    void sendMailAlert_shouldSendEmailAndMarkAlertSent() {
        // given: przygotowanie leku z datą ważności i bez wysłanego alertu
        DrugsEntity drug = new DrugsEntity();
        drug.setDrugsId(1);
        drug.setDrugsName("Old Drug");
        OffsetDateTime end = DateUtils.buildExpirationDate(2025, 5);
        drug.setExpirationDate(end);
        drug.setAlertSent(false);
        drug.setDrugsForm(DrugsFormEntity.builder().name("PILLS").build());

        // mockowanie repozytorium – zwróci lek zbliżający się do przeterminowania
        when(drugsRepository.findByExpirationDateLessThanEqualAndAlertSentFalse(eq(end)))
                .thenReturn(List.of(drug));

        // mockowanie metody wysyłającej e-mail
        doNothing().when(mailService).sendEmail(any(), any(), any());

        // mockowanie zapisu leku po wysłaniu alertu
        when(drugsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when: wykonanie metody wysyłającej alerty
        drugsService.sendExpiryAlertEmails(2025, 5);

        // then: sprawdzenie, że e-mail został wysłany dwa razy (na dwa adresy)
        verify(mailService, times(2)).sendEmail(anyString(), anyString(), anyString());

        // sprawdzenie, że lek został zapisany
        ArgumentCaptor<DrugsEntity> savedCaptor = ArgumentCaptor.forClass(DrugsEntity.class);
        verify(drugsRepository).save(savedCaptor.capture());

        // sprawdzenie, że znacznik alertu został ustawiony
        DrugsEntity saved = savedCaptor.getValue();
        assertThat(saved.getAlertSent()).isTrue();
    }
}
