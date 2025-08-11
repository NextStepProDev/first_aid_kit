package com.firstaid.infrastructure.database.mapper;

import com.firstaid.controller.dto.DrugDTO;
import com.firstaid.infrastructure.database.entity.DrugEntity;
import com.firstaid.infrastructure.database.mapper.helper.ExpirationDateMapperHelper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {DrugFormMapper.class, ExpirationDateMapperHelper.class})
public interface DrugMapper {

    DrugDTO mapToDTO(DrugEntity entity);
}