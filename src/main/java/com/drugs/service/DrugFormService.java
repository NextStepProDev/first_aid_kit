package com.drugs.service;

import com.drugs.controller.dto.DrugFormDTO;
import com.drugs.infrastructure.database.entity.DrugFormEntity;
import com.drugs.infrastructure.database.repository.DrugFormRepository;
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
        return drugFormRepository.findByName(dto.name()).orElseThrow();
    }
}