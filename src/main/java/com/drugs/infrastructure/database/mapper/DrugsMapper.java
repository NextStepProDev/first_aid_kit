package com.drugs.infrastructure.database.mapper;

import com.drugs.controller.dto.DrugSimpleDTO;
import com.drugs.controller.dto.DrugsDTO;
import com.drugs.infrastructure.database.entity.DrugsEntity;
import com.drugs.infrastructure.database.mapper.helper.ExpirationDateMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DrugsFormMapper.class, ExpirationDateMapperHelper.class})
public interface DrugsMapper {

    /** * Maps a DrugsEntity to a DrugsDTO.
     *
     * @param entity the DrugsEntity to map
     * @return the mapped DrugsDTO
     */
    DrugsDTO mapToDTO(DrugsEntity entity);

    /**
     * Maps a DrugsEntity to a DrugSimpleDTO.
     *
     * @param entity the DrugsEntity to map
     * @return the mapped DrugSimpleDTO
     */
    @Mapping(target = "expirationDate", source = "entity.expirationDate", qualifiedByName = "mapExpirationDateToYearMonth")
    DrugSimpleDTO mapToSimpleDTO(DrugsEntity entity);
}