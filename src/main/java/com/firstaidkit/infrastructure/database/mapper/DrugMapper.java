package com.firstaidkit.infrastructure.database.mapper;

import com.firstaidkit.controller.dto.drug.DrugResponse;
import com.firstaidkit.infrastructure.database.entity.DrugEntity;
import com.firstaidkit.infrastructure.database.mapper.helper.ExpirationDateMapperHelper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {DrugFormMapper.class, ExpirationDateMapperHelper.class})
public interface DrugMapper {

    DrugResponse mapToDTO(DrugEntity entity);
}