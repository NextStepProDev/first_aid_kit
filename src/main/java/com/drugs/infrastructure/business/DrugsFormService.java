package com.drugs.infrastructure.business;

import com.drugs.controller.dto.DrugsFormDTO;
import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import com.drugs.infrastructure.database.repository.DrugsFormRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DrugsFormService {

    private static final Logger logger = LoggerFactory.getLogger(DrugsFormService.class);

    private final DrugsFormRepository drugsFormRepository;

    public DrugsFormEntity resolve(DrugsFormDTO dto) {
        logger.info("Attempting to resolve drug form with name: {}", dto.name());

        return drugsFormRepository.findByName(dto.name())
                .orElseThrow(() -> {
                    logger.error("Drug form not found: {}", dto.name());
                    return new RuntimeException("Nie znaleziono formy leku: " + dto.name());
                });
    }
}