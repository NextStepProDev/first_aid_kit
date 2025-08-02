package com.drugs.infrastructure.database.mapper;

import com.drugs.controller.dto.DrugDTO;
import com.drugs.controller.dto.DrugSimpleDTO;
import com.drugs.infrastructure.database.entity.DrugEntity;
import com.drugs.infrastructure.database.mapper.helper.ExpirationDateMapperHelper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DrugFormMapper.class, ExpirationDateMapperHelper.class})
public interface DrugMapper {

    /** * Maps a DrugEntity to a DrugDTO.
     *
     * @param entity the DrugEntity to map
     * @return the mapped DrugDTO
     */
    DrugDTO mapToDTO(DrugEntity entity);

    /**
     * Maps a DrugEntity to a DrugSimpleDTO.
     *
     * @param entity the DrugEntity to map
     * @return the mapped DrugSimpleDTO
     */
    @Mapping(target = "expirationDate", source = "entity.expirationDate", qualifiedByName = "mapExpirationDateToYearMonth")
    DrugSimpleDTO mapToSimpleDTO(DrugEntity entity);
}