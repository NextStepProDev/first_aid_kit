package com.firstaidkit.service;

import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.infrastructure.database.entity.DrugFormEntity;
import com.firstaidkit.infrastructure.database.repository.DrugFormRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrugFormService {

    private final DrugFormRepository drugFormRepository;

    public DrugFormEntity resolve(DrugFormDTO dto) {
        log.info("Attempting to resolve drug form with name: {}", dto.name());
        return drugFormRepository.findByNameIgnoreCase(dto.name()).orElseThrow();
    }
}