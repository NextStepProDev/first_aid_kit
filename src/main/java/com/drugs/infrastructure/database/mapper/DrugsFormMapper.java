package com.drugs.infrastructure.database.mapper;

import com.drugs.controller.dto.DrugsFormDTO;
import com.drugs.infrastructure.database.entity.DrugsFormEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrugsFormMapper {

    default DrugsFormDTO toDTO(DrugsFormEntity entity) {
        if (entity == null) return null;
        return DrugsFormDTO.valueOf(entity.getName());
    }
}