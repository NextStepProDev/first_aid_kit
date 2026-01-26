package com.firstaid.infrastructure.database.mapper;

import com.firstaid.controller.dto.drug.DrugResponse;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.mapper.helper.ExpirationDateMapperHelper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {DrugFormMapper.class, ExpirationDateMapperHelper.class})
public interface DrugMapper {

    DrugResponse mapToDTO(DrugEntity entity);
}