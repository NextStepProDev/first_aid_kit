package com.firstaid.infrastructure.database.mapper;

import com.firstaid.controller.dto.DrugFormDTO;
import com.firstaid.infrastructure.database.entity.DrugFormEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DrugFormMapper {

    default DrugFormDTO toDTO(DrugFormEntity entity) {
        if (entity == null) return null;
        return DrugFormDTO.valueOf(entity.getName());
    }
}