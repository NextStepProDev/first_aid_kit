package com.drugs.infrastructure.business;

import com.drugs.controller.dto.DrugsFormDTO;
import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import com.drugs.infrastructure.database.repository.DrugsFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugsFormService {

    private final DrugsFormRepository drugsFormRepository;

    public DrugsFormEntity resolve(DrugsFormDTO dto) {
        log.info("Attempting to resolve drug form with name: {}", dto.name());
        return drugsFormRepository.findByName(dto.name()).orElseThrow();
    }
}