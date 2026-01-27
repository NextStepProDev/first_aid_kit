package com.firstaidkit.infrastructure.database.mapper;

import com.firstaidkit.controller.dto.drug.DrugFormDTO;
import com.firstaidkit.infrastructure.database.entity.DrugFormEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrugFormMapper {

    default DrugFormDTO toDTO(DrugFormEntity entity) {
        if (entity == null) return null;
        return DrugFormDTO.valueOf(entity.getName());
    }
}