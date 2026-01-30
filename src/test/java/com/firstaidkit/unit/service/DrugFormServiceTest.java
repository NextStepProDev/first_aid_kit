package com.firstaidkit.unit.service;

import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.infrastructure.database.entity.DrugFormEntity;
import com.firstaidkit.infrastructure.database.repository.DrugFormRepository;
import com.firstaidkit.service.DrugFormService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrugFormServiceTest {

    @Mock
    private DrugFormRepository drugFormRepository;

    @InjectMocks
    private DrugFormService drugFormService;

    // ---------------------- resolve ----------------------
    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        void shouldReturnEntityWhenFound() {
            DrugFormEntity entity = DrugFormEntity.builder().id(1).name("PILLS").build();
            when(drugFormRepository.findByNameIgnoreCase("PILLS")).thenReturn(Optional.of(entity));

            DrugFormEntity result = drugFormService.resolve(DrugFormDTO.PILLS);

            assertThat(result).isEqualTo(entity);
            assertThat(result.getName()).isEqualTo("PILLS");
        }

        @Test
        void shouldThrowWhenFormNotFound() {
            when(drugFormRepository.findByNameIgnoreCase("GEL")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> drugFormService.resolve(DrugFormDTO.GEL))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }
}
