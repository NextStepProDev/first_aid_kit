package com.drugs.infrastructure.database.mapper;

import com.drugs.controller.dto.DrugSimpleDTO;
import com.drugs.controller.dto.DrugsDTO;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.mapper.helper.ExpirationDateMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DrugsFormMapper.class, ExpirationDateMapperHelper.class})
public interface DrugsMapper {

    DrugsDTO mapToDTO(DrugsEntity entity);

    @Mapping(target = "alertSent", ignore = true)
    DrugsEntity mapFromDTO(DrugsDTO dto);

    @Mapping(target = "expirationDate", source = "entity.expirationDate", qualifiedByName = "mapExpirationDateToYearMonth")
    DrugSimpleDTO mapToSimpleDTO(DrugsEntity entity);
}