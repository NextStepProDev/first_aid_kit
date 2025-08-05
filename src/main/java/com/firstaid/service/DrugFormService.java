package com.firstaid.service;

import com.firstaid.controller.dto.DrugFormDTO;
import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import com.firstaid.infrastructure.database.repository.DrugFormRepository;
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