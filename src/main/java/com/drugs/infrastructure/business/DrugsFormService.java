package com.drugs.infrastructure.business;

import com.drugs.controller.dto.DrugsFormDTO;
import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import com.drugs.infrastructure.database.repository.DrugsFormRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrugsFormService {

    private final DrugsFormRepository drugsFormRepository;

    public DrugsFormEntity resolve(DrugsFormDTO dto) {
        return drugsFormRepository.findByName(dto.name())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono formy leku: " + dto.name()));
    }
}