package com.drugs.infrastructure.database.mapper;

import com.drugs.controller.dto.DrugSimpleDTO;
import com.drugs.controller.dto.DrugsDTO;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = DrugsFormMapper.class)
public interface DrugsMapper {

    DrugsDTO mapToDTO(DrugsEntity entity);

    @Mapping(target = "alertSent", ignore = true)
    DrugsEntity mapFromDTO(DrugsDTO dto);

    @Mapping(target = "expirationDate", expression = "java(entity.getExpirationDate().getYear() + \"-\" + " +
            "String.format(\"%02d\", entity.getExpirationDate().getMonthValue()))")
    DrugSimpleDTO mapToSimpleDTO(DrugsEntity entity);
}